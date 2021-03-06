/*
 * (C) Copyright 2021 Radix DLT Ltd
 *
 * Radix DLT Ltd licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 */

package com.radixdlt.application.tokens;

import com.radixdlt.application.system.construction.CreateSystemConstructorV2;
import com.radixdlt.application.system.scrypt.EpochUpdateConstraintScrypt;
import com.radixdlt.application.system.scrypt.RoundUpdateConstraintScrypt;
import com.radixdlt.application.system.scrypt.SystemConstraintScrypt;
import com.radixdlt.application.validators.scrypt.ValidatorUpdateOwnerConstraintScrypt;
import com.radixdlt.atom.ActionConstructor;
import com.radixdlt.atom.REConstructor;
import com.radixdlt.atom.TxBuilderException;
import com.radixdlt.atom.TxnConstructionRequest;
import com.radixdlt.atom.actions.CreateMutableToken;
import com.radixdlt.atom.actions.CreateSystem;
import com.radixdlt.atom.actions.MintToken;
import com.radixdlt.atom.actions.StakeTokens;
import com.radixdlt.atom.actions.UpdateAllowDelegationFlag;
import com.radixdlt.atom.actions.UpdateValidatorOwner;
import com.radixdlt.application.tokens.construction.CreateMutableTokenConstructor;
import com.radixdlt.application.tokens.construction.MintTokenConstructor;
import com.radixdlt.application.tokens.construction.StakeTokensConstructorV3;
import com.radixdlt.application.tokens.scrypt.StakingConstraintScryptV4;
import com.radixdlt.application.tokens.scrypt.TokensConstraintScryptV3;
import com.radixdlt.application.validators.construction.UpdateAllowDelegationFlagConstructor;
import com.radixdlt.application.validators.construction.UpdateValidatorOwnerConstructor;
import com.radixdlt.application.validators.scrypt.ValidatorConstraintScryptV2;
import com.radixdlt.atomos.CMAtomOS;
import com.radixdlt.atomos.ConstraintScrypt;
import com.radixdlt.constraintmachine.ConstraintMachine;
import com.radixdlt.constraintmachine.PermissionLevel;
import com.radixdlt.crypto.ECKeyPair;
import com.radixdlt.engine.RadixEngine;
import com.radixdlt.engine.parser.REParser;
import com.radixdlt.identifiers.REAddr;
import com.radixdlt.store.EngineStore;
import com.radixdlt.store.InMemoryEngineStore;
import com.radixdlt.utils.Pair;
import com.radixdlt.utils.UInt256;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(Parameterized.class)
public class DelegationFlagTest {

	@Parameterized.Parameters
	public static Collection<Object[]> parameters() {
		var startAmounts = List.of(10);
		var stakeAmounts = List.of(10);
		var scrypts = List.of(
			Pair.of(
				List.of(
					new RoundUpdateConstraintScrypt(10),
					new EpochUpdateConstraintScrypt(10, UInt256.NINE, 1, 1, 10),
					new TokensConstraintScryptV3(),
					new StakingConstraintScryptV4(Amount.ofTokens(10).toSubunits()),
					new ValidatorConstraintScryptV2(),
					new ValidatorUpdateOwnerConstraintScrypt()
				),
				new StakeTokensConstructorV3(Amount.ofTokens(10).toSubunits())
			)
		);

		var parameters = new ArrayList<Object[]>();
		for (var scrypt : scrypts) {
			for (var startAmount : startAmounts) {
				for (var stakeAmount : stakeAmounts) {
					var param = new Object[] {
						startAmount,
						stakeAmount,
						scrypt.getFirst(),
						scrypt.getSecond()
					};
					parameters.add(param);
				}
			}
		}
		return parameters;
	}


	private RadixEngine<Void> engine;
	private EngineStore<Void> store;
	private final UInt256 startAmt;
	private final UInt256 stakeAmt;
	private final List<ConstraintScrypt> scrypts;
	private final ActionConstructor<StakeTokens> stakeTokensConstructor;

	public DelegationFlagTest(
		long startAmt,
		long stakeAmt,
		List<ConstraintScrypt> scrypts,
		ActionConstructor<StakeTokens> stakeTokensConstructor
	) {
		this.startAmt = Amount.ofTokens(startAmt * 10).toSubunits();
		this.stakeAmt = Amount.ofTokens(stakeAmt * 10).toSubunits();
		this.scrypts = scrypts;
		this.stakeTokensConstructor = stakeTokensConstructor;
	}

	@Before
	public void setup() throws Exception {
		var cmAtomOS = new CMAtomOS();
		cmAtomOS.load(new SystemConstraintScrypt(Set.of()));
		scrypts.forEach(cmAtomOS::load);
		var cm = new ConstraintMachine(
			cmAtomOS.getProcedures(),
			cmAtomOS.buildSubstateDeserialization(),
			cmAtomOS.buildVirtualSubstateDeserialization()
		);
		var parser = new REParser(cmAtomOS.buildSubstateDeserialization());
		var serialization = cmAtomOS.buildSubstateSerialization();
		this.store = new InMemoryEngineStore<>();
		this.engine = new RadixEngine<>(
			parser,
			serialization,
			REConstructor.newBuilder()
				.put(StakeTokens.class, stakeTokensConstructor)
				.put(CreateMutableToken.class, new CreateMutableTokenConstructor())
				.put(MintToken.class, new MintTokenConstructor())
				.put(UpdateAllowDelegationFlag.class, new UpdateAllowDelegationFlagConstructor())
				.put(UpdateValidatorOwner.class, new UpdateValidatorOwnerConstructor())
				.put(CreateSystem.class, new CreateSystemConstructorV2())
				.build(),
			cm,
			store
		);
		var txn = this.engine.construct(new CreateSystem(0)).buildWithoutSignature();
		this.engine.execute(List.of(txn), null, PermissionLevel.SYSTEM);
	}

	@Test
	public void cannot_construct_stake_tokens_if_delegation_flag_set_to_false() throws Exception {
		// Arrange
		var key = ECKeyPair.generateNew();
		var accountAddr = REAddr.ofPubKeyAccount(key.getPublicKey());
		var txn = this.engine.construct(
			TxnConstructionRequest.create()
				.action(new CreateMutableToken(null, "xrd", "Name", "", "", ""))
				.action(new MintToken(REAddr.ofNativeToken(), accountAddr, startAmt))
		).buildWithoutSignature();
		var validatorKey = ECKeyPair.generateNew();
		this.engine.execute(List.of(txn), null, PermissionLevel.SYSTEM);

		// Act
		assertThatThrownBy(() -> this.engine.construct(new StakeTokens(accountAddr, validatorKey.getPublicKey(), stakeAmt))
			.signAndBuild(key::sign)).isInstanceOf(TxBuilderException.class);
	}

	@Test
	public void can_stake_tokens_if_delegation_flag_set_to_false_and_am_owner() throws Exception {
		// Arrange
		var key = ECKeyPair.generateNew();
		var accountAddr = REAddr.ofPubKeyAccount(key.getPublicKey());
		var txn = this.engine.construct(
			TxnConstructionRequest.create()
				.action(new CreateMutableToken(null, "xrd", "Name", "", "", ""))
				.action(new MintToken(REAddr.ofNativeToken(), accountAddr, startAmt))
		).buildWithoutSignature();
		this.engine.execute(List.of(txn), null, PermissionLevel.SYSTEM);

		// Act
		var stake = this.engine.construct(new StakeTokens(accountAddr, key.getPublicKey(), stakeAmt))
			.signAndBuild(key::sign);
		this.engine.execute(List.of(stake));
	}


	@Test
	public void can_stake_tokens_if_delegation_flag_set_to_false_and_changed_owner() throws Exception {
		// Arrange
		var key = ECKeyPair.generateNew();
		var accountAddr = REAddr.ofPubKeyAccount(key.getPublicKey());
		var txn = this.engine.construct(
			TxnConstructionRequest.create()
				.action(new CreateMutableToken(null, "xrd", "Name", "", "", ""))
				.action(new MintToken(REAddr.ofNativeToken(), accountAddr, startAmt))
		).buildWithoutSignature();
		var validatorKey = ECKeyPair.generateNew();
		this.engine.execute(List.of(txn), null, PermissionLevel.SYSTEM);
		var update = this.engine.construct(
			TxnConstructionRequest.create()
				.action(new UpdateValidatorOwner(validatorKey.getPublicKey(), accountAddr))
		).signAndBuild(validatorKey::sign);
		this.engine.execute(List.of(update));

		// Act
		var stake = this.engine.construct(new StakeTokens(accountAddr, key.getPublicKey(), stakeAmt))
			.signAndBuild(key::sign);
		this.engine.execute(List.of(stake));
	}
}
