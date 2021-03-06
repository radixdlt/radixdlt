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

package com.radixdlt.integration.distributed.simulation.tests.full_function;

import com.radixdlt.application.tokens.Amount;
import com.radixdlt.integration.distributed.simulation.NetworkLatencies;
import com.radixdlt.integration.distributed.simulation.NetworkOrdering;
import com.radixdlt.integration.distributed.simulation.SimulationTest;
import com.radixdlt.integration.distributed.simulation.application.RadixEngineUniqueGenerator;
import com.radixdlt.integration.distributed.simulation.monitors.consensus.ConsensusMonitors;
import com.radixdlt.integration.distributed.simulation.monitors.ledger.LedgerMonitors;
import com.radixdlt.integration.distributed.simulation.monitors.radix_engine.RadixEngineMonitors;
import com.radixdlt.mempool.MempoolConfig;
import com.radixdlt.application.system.FeeTable;
import com.radixdlt.statecomputer.forks.ForksModule;
import com.radixdlt.statecomputer.forks.MainnetForkConfigsModule;
import com.radixdlt.statecomputer.forks.RERulesConfig;
import com.radixdlt.statecomputer.forks.RadixEngineForksLatestOnlyModule;
import com.radixdlt.sync.SyncConfig;
import com.radixdlt.utils.UInt256;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@RunWith(Parameterized.class)
public class OneOutOfBoundsTest {
	@Parameterized.Parameters
	public static Collection<Object[]> fees() {
		return List.of(new Object[][] {
			{UInt256.ONE}, {UInt256.ZERO},
		});
	}

	private final SimulationTest.Builder bftTestBuilder;

	public OneOutOfBoundsTest(UInt256 perByteFee) {
		bftTestBuilder = SimulationTest.builder()
			.numNodes(4)
			.pacemakerTimeout(3000)
			.networkModules(
				NetworkOrdering.inOrder(),
				NetworkLatencies.oneOutOfBounds(50, 10000)
			)
			.fullFunctionNodes(SyncConfig.of(400L, 10, 2000L))
			.addRadixEngineConfigModules(
				new MainnetForkConfigsModule(),
				new RadixEngineForksLatestOnlyModule(
					new RERulesConfig(
						FeeTable.create(
							Amount.ofSubunits(perByteFee),
							Map.of()
						),
						OptionalInt.of(5),
						20L,
						2,
						Amount.ofTokens(10),
						1,
						Amount.ofTokens(10),
						9800,
						10
					)),
				new ForksModule()
			)
			.addNodeModule(MempoolConfig.asModule(1000, 10))
			.addTestModules(
				ConsensusMonitors.safety(),
				ConsensusMonitors.liveness(10000, TimeUnit.SECONDS),
				LedgerMonitors.consensusToLedger(),
				LedgerMonitors.ordered(),
				RadixEngineMonitors.noInvalidProposedCommands()
			)
			.addMempoolSubmissionsSteadyState(RadixEngineUniqueGenerator.class);
	}


	@Test
	public void sanity_tests_should_pass() {
		SimulationTest simulationTest = bftTestBuilder
			.build();

		final var results = simulationTest.run(Duration.ofMinutes(2)).awaitCompletion();
		assertThat(results).allSatisfy((name, err) -> AssertionsForClassTypes.assertThat(err).isEmpty());
	}
}
