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

package com.radixdlt.integration.distributed.simulation.monitors.application;

import com.radixdlt.atom.Txn;
import com.radixdlt.consensus.bft.BFTCommittedUpdate;
import com.radixdlt.consensus.bft.PreparedVertex;
import com.radixdlt.integration.distributed.simulation.TestInvariant;
import com.radixdlt.integration.distributed.simulation.monitors.NodeEvents;
import com.radixdlt.integration.distributed.simulation.network.SimulationNodes.RunningNetwork;
import io.reactivex.rxjava3.core.Observable;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Checks to make sure that commands have been committed in a certain amount
 * of time
 */
public class CommittedChecker implements TestInvariant {
	private static final Logger log = LogManager.getLogger();
	private final Observable<Txn> submittedTxns;
	private final NodeEvents commits;

	public CommittedChecker(Observable<Txn> submittedTxns, NodeEvents commits) {
		this.submittedTxns = Objects.requireNonNull(submittedTxns);
		this.commits = Objects.requireNonNull(commits);
	}

	@Override
	public Observable<TestInvariantError> check(RunningNetwork network) {
		return submittedTxns
			.doOnNext(txn -> log.debug("Submitted txn: {}", txn))
			.flatMapMaybe(txn ->
				Observable.<BFTCommittedUpdate>create(
					emitter -> commits.addListener((n, e) -> emitter.onNext(e), BFTCommittedUpdate.class))
					.serialize()
					.filter(e -> e.getCommitted().stream()
						.flatMap(PreparedVertex::getTxns)
						.anyMatch(c -> Arrays.equals(c.getPayload(), txn.getPayload())))
					.timeout(10, TimeUnit.SECONDS)
					.firstOrError()
					.ignoreElement()
					.onErrorReturn(e -> new TestInvariantError(
						"Submitted txn has not been committed in 10 seconds: " + txn
					))
			);
	}
}
