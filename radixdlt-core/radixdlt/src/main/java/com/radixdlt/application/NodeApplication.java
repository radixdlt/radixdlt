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

package com.radixdlt.application;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.radixdlt.atom.TxBuilderException;
import com.radixdlt.consensus.HashSigner;
import com.radixdlt.consensus.bft.Self;
import com.radixdlt.engine.RadixEngine;
import com.radixdlt.environment.EventDispatcher;
import com.radixdlt.environment.EventProcessor;
import com.radixdlt.identifiers.RadixAddress;
import com.radixdlt.mempool.MempoolAdd;
import com.radixdlt.statecomputer.LedgerAndBFTProof;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NodeApplication {
	private static final Logger log = LogManager.getLogger();

	private final RadixAddress self;
	private final RadixEngine<LedgerAndBFTProof> radixEngine;
	private final HashSigner hashSigner;
	private final EventDispatcher<MempoolAdd> mempoolAddEventDispatcher;

	@Inject
	public NodeApplication(
		@Self RadixAddress self,
		@Named("RadixEngine") HashSigner hashSigner,
		RadixEngine<LedgerAndBFTProof> radixEngine,
		EventDispatcher<MempoolAdd> mempoolAddEventDispatcher
	) {
		this.self = self;
		this.hashSigner = hashSigner;
		this.radixEngine = radixEngine;
		this.mempoolAddEventDispatcher = mempoolAddEventDispatcher;
	}

	private void processRequest(NodeApplicationRequest request) {
		log.info("NodeServiceRequest {}", request);

		try {
			var txBuilder = radixEngine.construct(self, request.getActions());
			var txn = txBuilder.signAndBuild(hashSigner::sign);
			this.mempoolAddEventDispatcher.dispatch(MempoolAdd.create(txn));
			request.onSuccess(txn.getId());
		} catch (TxBuilderException e) {
			log.error("Faucet failed to fulfil request {}", request, e);
			request.onFailure(e.getMessage());
		}
	}

	public EventProcessor<NodeApplicationRequest> requestEventProcessor() {
		return this::processRequest;
	}
}