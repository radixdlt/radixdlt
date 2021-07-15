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

import com.radixdlt.consensus.bft.View;
import com.radixdlt.statecomputer.forks.Forks;
import com.radixdlt.statecomputer.forks.RERules;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1OutputStream;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.util.encoders.Hex;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import com.radixdlt.api.data.PreparedTransaction;
import com.radixdlt.api.service.ActionParserService;
import com.radixdlt.api.service.SubmissionService;
import com.radixdlt.atom.Txn;
import com.radixdlt.crypto.ECDSASignature;
import com.radixdlt.crypto.ECKeyPair;
import com.radixdlt.crypto.ECPublicKey;
import com.radixdlt.crypto.HashUtils;
import com.radixdlt.identifiers.REAddr;
import com.radixdlt.networks.Addressing;
import com.radixdlt.networks.Network;
import com.radixdlt.utils.UInt256;
import com.radixdlt.utils.functional.Result;

import java.io.ByteArrayOutputStream;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static com.radixdlt.api.JsonRpcUtil.jsonArray;
import static com.radixdlt.api.JsonRpcUtil.jsonObject;

public class ConstructionHandlerTest {
	private static final ECPublicKey PUB_KEY = ECKeyPair.generateNew().getPublicKey();
	private static final REAddr ACCOUNT_ADDR = REAddr.ofPubKeyAccount(PUB_KEY);
	private static final Addressing addressing = Addressing.ofNetwork(Network.LOCALNET);
	private static final String FEE_PAYER = addressing.forAccounts().of(ACCOUNT_ADDR);

	private final SubmissionService submissionService = mock(SubmissionService.class);
	private final Forks forks = mock(Forks.class);
	private final ActionParserService actionParserService = new ActionParserService(addressing, forks);
	private final ConstructionHandler handler = new ConstructionHandler(submissionService, actionParserService, addressing);

	@Before
	public void setup() {
		final var reRules = mock(RERules.class);
		when(reRules.getMaxRounds()).thenReturn(View.of(10L));
		when(forks.getCandidateFork()).thenReturn(Optional.empty());
	}

	@Test
	public void testBuildTransactionPositional() {
		var prepared = PreparedTransaction.create(randomBytes(), randomBytes(), UInt256.TEN);

		when(submissionService.prepareTransaction(any(), any(), any(), eq(false)))
			.thenReturn(Result.ok(prepared));

		var actions = jsonArray()
			.put(
				jsonObject()
					.put("type", "RegisterValidator")
					.put("validator", addressing.forValidators().of(PUB_KEY))
			);
		var params = jsonArray()
			.put(actions)
			.put(FEE_PAYER)
			.put("message");

		var response = handler.handleConstructionBuildTransaction(requestWith(params));

		assertNotNull(response);
		assertTrue(response.has("result"));

		var result = response.getJSONObject("result");

		assertNotNull(result);
		assertEquals("10", result.get("fee"));
	}

	@Test
	public void testBuildTransactionNamed() {
		var prepared = PreparedTransaction.create(randomBytes(), randomBytes(), UInt256.TEN);

		when(submissionService.prepareTransaction(any(), any(), any(), eq(false)))
			.thenReturn(Result.ok(prepared));

		var actions = jsonArray()
			.put(
				jsonObject()
					.put("type", "RegisterValidator")
					.put("validator", addressing.forValidators().of(PUB_KEY))
			);
		var params = jsonObject()
			.put("actions", actions)
			.put("feePayer", FEE_PAYER)
			.put("message", "message");

		var response = handler.handleConstructionBuildTransaction(requestWith(params));

		assertNotNull(response);
		assertTrue(response.has("result"));

		var result = response.getJSONObject("result");

		assertNotNull(result);
		assertEquals("10", result.get("fee"));
	}

	@Test
	public void testFinalizeTransactionPositional() {
		var txn = Txn.create(randomBytes());

		when(submissionService.finalizeTxn(any(), any(), anyBoolean()))
			.thenReturn(Result.ok(txn));

		var blob = randomBytes();
		var hash = HashUtils.sha256(blob).asBytes();
		var keyPair = ECKeyPair.generateNew();
		var signature = keyPair.sign(hash);
		var params = jsonArray()
			.put(Hex.toHexString(blob))
			.put(encodeToDer(signature))
			.put(keyPair.getPublicKey().toHex());

		var response = handler.handleConstructionFinalizeTransaction(requestWith(params));

		assertNotNull(response);
		assertTrue(response.has("result"));

		var result = response.getJSONObject("result");

		assertTrue(result.has("txID"));
		assertTrue(result.has("blob"));

		assertEquals(txn.getId(), result.get("txID"));
		assertEquals(Hex.toHexString(txn.getPayload()), result.get("blob"));
	}

	@Test
	public void testFinalizeTransactionNamed() {
		var txn = Txn.create(randomBytes());

		when(submissionService.finalizeTxn(any(), any(), anyBoolean()))
			.thenReturn(Result.ok(txn));

		var blob = randomBytes();
		var hash = HashUtils.sha256(blob).asBytes();
		var keyPair = ECKeyPair.generateNew();
		var signature = keyPair.sign(hash);
		var params = jsonObject()
			.put("blob", Hex.toHexString(blob))
			.put("signatureDER", encodeToDer(signature))
			.put("publicKeyOfSigner", keyPair.getPublicKey().toHex());

		var response = handler.handleConstructionFinalizeTransaction(requestWith(params));

		assertNotNull(response);
		assertTrue(response.has("result"));

		var result = response.getJSONObject("result");

		assertTrue(result.has("txID"));
		assertTrue(result.has("blob"));

		assertEquals(txn.getId(), result.get("txID"));
		assertEquals(Hex.toHexString(txn.getPayload()), result.get("blob"));
	}

	@Test
	public void testSubmitTransactionPositional() {
		var blob = randomBytes();
		var txn = Txn.create(blob);

		when(submissionService.submitTx(any(), any()))
			.thenReturn(Result.ok(txn));

		var params = jsonArray()
			.put(Hex.toHexString(blob))
			.put(txn.getId().toString());

		var response = handler.handleConstructionSubmitTransaction(requestWith(params));

		assertNotNull(response);
		assertTrue(response.has("result"));

		var result = response.getJSONObject("result");

		assertTrue(result.has("txID"));
		assertEquals(txn.getId(), result.get("txID"));
	}

	@Test
	public void testSubmitTransactionNamed() {
		var blob = randomBytes();
		var txn = Txn.create(blob);

		when(submissionService.submitTx(any(), any()))
			.thenReturn(Result.ok(txn));

		var params = jsonObject()
			.put("blob", Hex.toHexString(blob))
			.put("txID", txn.getId().toJson());

		var response = handler.handleConstructionSubmitTransaction(requestWith(params));

		assertNotNull(response);
		assertTrue(response.has("result"));

		var result = response.getJSONObject("result");

		assertTrue(result.has("txID"));

		assertEquals(txn.getId(), result.get("txID"));
	}

	private String encodeToDer(ECDSASignature signature) {
		try {
			var vector = new ASN1EncodableVector();
			vector.add(new ASN1Integer(signature.getR()));
			vector.add(new ASN1Integer(signature.getS()));

			var baos = new ByteArrayOutputStream();
			var asnOS = ASN1OutputStream.create(baos);
			asnOS.writeObject(new DERSequence(vector));
			asnOS.flush();

			return Hex.toHexString(baos.toByteArray());
		} catch (Exception e) {
			fail(e.getMessage());
			return null;
		}
	}

	private byte[] randomBytes() {
		return HashUtils.random256().asBytes();
	}

	private JSONObject requestWith(Object params) {
		return jsonObject().put("id", "1").putOpt("params", params);
	}
}