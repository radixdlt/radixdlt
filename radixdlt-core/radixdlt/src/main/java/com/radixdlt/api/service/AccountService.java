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

package com.radixdlt.api.service;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.inject.Inject;
import com.radixdlt.api.Rri;
import com.radixdlt.api.data.BalanceEntry;
import com.radixdlt.api.data.TxHistoryEntry;
import com.radixdlt.api.data.UnstakeEntry;
import com.radixdlt.api.store.ClientApiStore;
import com.radixdlt.api.store.TokenBalance;
import com.radixdlt.application.Balances;
import com.radixdlt.application.StakeReceived;
import com.radixdlt.application.StakedBalance;
import com.radixdlt.application.ValidatorInfo;
import com.radixdlt.consensus.bft.Self;
import com.radixdlt.crypto.ECPublicKey;
import com.radixdlt.engine.RadixEngine;
import com.radixdlt.identifiers.AccountAddress;
import com.radixdlt.identifiers.REAddr;
import com.radixdlt.identifiers.ValidatorAddress;
import com.radixdlt.statecomputer.LedgerAndBFTProof;
import com.radixdlt.utils.Pair;
import com.radixdlt.utils.UInt256;
import com.radixdlt.utils.UInt384;
import com.radixdlt.utils.functional.Result;
import com.radixdlt.utils.functional.Tuple.Tuple2;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.radixdlt.api.JsonRpcUtil.jsonArray;
import static com.radixdlt.api.JsonRpcUtil.jsonObject;
import static com.radixdlt.utils.functional.Tuple.tuple;

public class AccountService {
	private final RadixEngine<LedgerAndBFTProof> radixEngine;
	private final ECPublicKey bftKey;
	private final ClientApiStore clientApiStore;

	@Inject
	public AccountService(
		RadixEngine<LedgerAndBFTProof> radixEngine,
		@Self ECPublicKey bftKey,
		ClientApiStore clientApiStore
	) {
		this.radixEngine = radixEngine;
		this.bftKey = bftKey;
		this.clientApiStore = clientApiStore;
	}

	public Result<List<TokenBalance>> getTokenBalances(REAddr addr) {
		return clientApiStore.getTokenBalances(addr, false)
			.map(list -> list.stream().map(TokenBalance::from).collect(Collectors.toList()));
	}

	public Result<Tuple2<Optional<Instant>, List<TxHistoryEntry>>> getTransactionHistory(
		REAddr address, int size, Optional<Instant> cursor
	) {
		return clientApiStore.getTransactionHistory(address, size, cursor)
			.map(response -> tuple(calculateNewCursor(response), response));
	}

	public Result<List<BalanceEntry>> getStakePositions(REAddr addr) {
		return clientApiStore.getTokenBalances(addr, true);
	}

	//TODO: restore functionality, not everything is spendable anymore
	public Result<List<UnstakeEntry>> getUnstakePositions(REAddr addr) {
		//return Result.ok(List.of());
	}

	public JSONObject getAccountInfo() {
		return jsonObject()
			.put("address", getOwnAddress())
			.put("balance", getOwnBalance());
	}

	public JSONObject getValidatorInfo() {
		var validatorInfo = getValidatorDetails();
		var validatorStakes = getValidatorStakes();

		return new JSONObject()
			.put("address", ValidatorAddress.of(bftKey))
			.put("name", validatorInfo.getName())
			.put("url", validatorInfo.getUrl())
			.put("registered", validatorInfo.isRegistered())
			.put("stakes", validatorStakes.getFirst())
			.put("totalStake", validatorStakes.getSecond());
	}

	private ValidatorInfo getValidatorDetails() {
		return radixEngine.getComputedState(ValidatorInfo.class);
	}

	private Pair<UInt256, JSONArray> getValidatorStakes() {
		var stakeReceived = radixEngine.getComputedState(StakeReceived.class);
		var stakeFrom = jsonArray();

		stakeReceived.forEach((addr, amt) -> {
			stakeFrom.put(
				jsonObject()
					.put("delegator", AccountAddress.of(addr))
					.put("amount", amt)
			);
		});

		return Pair.of(stakeReceived.getTotal(), stakeFrom);
	}

	private String getOwnAddress() {
		return AccountAddress.of(REAddr.ofPubKeyAccount(bftKey));
	}

	private JSONObject getOwnBalance() {
		var balances = radixEngine.getComputedState(Balances.class);
		var stakedBalance = radixEngine.getComputedState(StakedBalance.class);

		var stakesArray = jsonArray();
		stakedBalance.forEach((publicKey, amount) -> stakesArray.put(constructStakeEntry(publicKey, amount)));

		var balancesArray = jsonArray();
		balances.forEach((rri, amount) -> balancesArray.put(constructBalanceEntry(rri, amount)));

		return jsonObject()
			.put("tokens", balancesArray)
			.put("stakes", stakesArray);
	}

	private JSONObject constructBalanceEntry(REAddr rri, UInt384 amount) {
		return clientApiStore.getTokenDefinition(rri)
			.fold(
				__ -> jsonObject().put("rri", "<unknown>").put("amount", amount),
				definition -> jsonObject().put("rri", Rri.of(definition.getSymbol(), rri)).put("amount", amount)
			);
	}

	private JSONObject constructStakeEntry(ECPublicKey publicKey, UInt256 amount) {
		return jsonObject().put("delegate", ValidatorAddress.of(publicKey)).put("amount", amount);
	}

	private static Optional<Instant> calculateNewCursor(List<TxHistoryEntry> response) {
		return response.stream()
			.reduce(AccountService::findLast)
			.map(TxHistoryEntry::timestamp);
	}

	private static <T> T findLast(T first, T second) {
		return second;
	}
}