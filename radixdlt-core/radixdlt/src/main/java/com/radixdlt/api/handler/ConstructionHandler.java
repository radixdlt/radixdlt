/*
 * (C) Copyright 2021 Radix DLT Ltd
 *
 * Radix DLT Ltd licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the License.
 */

package com.radixdlt.api.handler;

import org.json.JSONObject;

import com.google.inject.Inject;
import com.radixdlt.api.data.PreparedTransaction;
import com.radixdlt.api.service.ActionParserService;
import com.radixdlt.api.service.SubmissionService;
import com.radixdlt.atom.Txn;
import com.radixdlt.crypto.ECDSASignature;
import com.radixdlt.crypto.ECKeyUtils;
import com.radixdlt.crypto.ECPublicKey;
import com.radixdlt.crypto.HashUtils;
import com.radixdlt.identifiers.AID;
import com.radixdlt.identifiers.REAddr;
import com.radixdlt.networks.Addressing;
import com.radixdlt.utils.functional.Result;
import com.radixdlt.utils.functional.Result.Mapper2;

import java.util.List;
import java.util.Optional;

import static org.bouncycastle.util.encoders.Hex.toHexString;

import static com.radixdlt.api.JsonRpcUtil.jsonObject;
import static com.radixdlt.api.JsonRpcUtil.optString;
import static com.radixdlt.api.JsonRpcUtil.safeArray;
import static com.radixdlt.api.JsonRpcUtil.safeBlob;
import static com.radixdlt.api.JsonRpcUtil.safeString;
import static com.radixdlt.api.JsonRpcUtil.withRequiredParameters;
import static com.radixdlt.api.data.ApiErrors.INVALID_SIGNATURE_DER;
import static com.radixdlt.api.data.ApiErrors.INVALID_TX_ID;
import static com.radixdlt.identifiers.CommonErrors.INVALID_PUBLIC_KEY;
import static com.radixdlt.utils.functional.Result.allOf;
import static com.radixdlt.utils.functional.Result.fromOptional;
import static com.radixdlt.utils.functional.Result.wrap;
import static com.radixdlt.utils.functional.Tuple.tuple;

public class ConstructionHandler {
	private final SubmissionService submissionService;
	private final ActionParserService actionParserService;
	private final Addressing addressing;

	@Inject
	public ConstructionHandler(
		SubmissionService submissionService,
		ActionParserService actionParserService,
		Addressing addressing
	) {
		this.submissionService = submissionService;
		this.actionParserService = actionParserService;
		this.addressing = addressing;
	}

	public JSONObject handleConstructionBuildTransaction(JSONObject request) {
		return withRequiredParameters(
			request,
			List.of("actions", "feePayer"),
			List.of("message", "disableResourceAllocationAndDestroy"),
			params ->
				allOf(safeArray(params, "actions"), account(params))
					.flatMap((actions, feePayer) -> actionParserService.parse(actions)
						.flatMap(steps -> submissionService.prepareTransaction(
							feePayer,
							steps,
							optString(params, "message"),
							params.optBoolean("disableResourceAllocationAndDestroy")
						))
						.map(PreparedTransaction::asJson))
		);
	}

	public JSONObject handleConstructionFinalizeTransaction(JSONObject request) {
		return withRequiredParameters(
			request,
			List.of("blob", "signatureDER", "publicKeyOfSigner"),
			List.of("immediateSubmit"),
			params ->
				allOf(parseBlob(params), parseSignatureDer(params), parsePublicKey(params))
					.flatMap((blob, signature, publicKey) -> toRecoverable(blob, signature, publicKey)
						.flatMap(recoverable -> submissionService.finalizeTxn(
							blob, recoverable, params.optBoolean("immediateSubmit")
						)))
					.map(ConstructionHandler::formatTx)
		);
	}

	public JSONObject handleConstructionSubmitTransaction(JSONObject request) {
		return withRequiredParameters(
			request,
			List.of("blob"),
			List.of("txID"),
			params ->
				safeBlob(params, "blob")
					.flatMap(blob -> parseTxId(blob, params)
						.flatMap(submissionService::submitTx))
					.map(Txn::getId)
					.map(ConstructionHandler::formatTxId)
		);
	}

	private static Result<ECDSASignature> toRecoverable(byte[] blob, ECDSASignature signature, ECPublicKey publicKey) {
		return ECKeyUtils.toRecoverable(signature, HashUtils.sha256(blob).asBytes(), publicKey);
	}

	private static Result<byte[]> parseBlob(JSONObject params) {
		return safeBlob(params, "blob");
	}

	private static Result<ECDSASignature> parseSignatureDer(JSONObject params) {
		return safeBlob(params, "signatureDER")
			.flatMap(param -> wrap(INVALID_SIGNATURE_DER, () -> ECDSASignature.decodeFromDER(param)));
	}

	private static Result<ECPublicKey> parsePublicKey(JSONObject params) {
		return safeBlob(params, "publicKeyOfSigner")
			.flatMap(param -> wrap(INVALID_PUBLIC_KEY, () -> ECPublicKey.fromBytes(param)));
	}

	private static Mapper2<byte[], Optional<AID>> parseTxId(byte[] blob, JSONObject params) {
		var optTxId = optString(params, "txID");

		if (optTxId.isEmpty()) {
			return () -> Result.ok(tuple(blob, Optional.empty()));
		}

		return () -> fromOptional(INVALID_TX_ID.with("txID"), optTxId)
			.flatMap(AID::fromString)
			.map(aid -> tuple(blob, Optional.of(aid)));
	}

	private Result<REAddr> account(JSONObject params) {
		return safeString(params, "feePayer")
			.flatMap(addressing.forAccounts()::parseFunctional);
	}

	private static JSONObject formatTxId(AID txId) {
		return jsonObject().put("txID", txId);
	}

	private static JSONObject formatTx(Txn txn) {
		return jsonObject()
			.put("txID", txn.getId())
			.put("blob", toHexString(txn.getPayload()));
	}
}
