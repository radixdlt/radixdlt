/*
 * (C) Copyright 2020 Radix DLT Ltd
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
 */

package com.radixdlt.ledger;

import com.radixdlt.consensus.BFTHeader;
import com.radixdlt.consensus.HashVerifier;
import com.radixdlt.consensus.Hasher;
import com.radixdlt.consensus.TimestampedECDSASignature;
import com.radixdlt.consensus.TimestampedVoteData;
import com.radixdlt.consensus.VerifiedLedgerHeaderAndProof;
import com.radixdlt.consensus.VoteData;
import com.radixdlt.consensus.bft.BFTNode;
import com.radixdlt.consensus.bft.BFTValidatorSet;
import com.radixdlt.consensus.bft.ValidationState;
import com.radixdlt.consensus.bft.View;
import com.radixdlt.crypto.Hash;
import com.radixdlt.sync.AccumulatorLocalSyncServiceProcessor.DtoCommandsAndProofVerifier;
import com.radixdlt.sync.AccumulatorLocalSyncServiceProcessor.DtoCommandsAndProofVerifierException;
import java.util.Map;
import java.util.Objects;

/**
 * Verifies the accumulator and validator set signatures
 */
public final class AccumulatorAndValidatorSetVerifier implements DtoCommandsAndProofVerifier {
	private final LedgerAccumulatorVerifier accumulatorVerifier;
	private final BFTValidatorSet validatorSet;
	private final Hasher hasher;
	private final HashVerifier hashVerifier;

	public AccumulatorAndValidatorSetVerifier(
		LedgerAccumulatorVerifier accumulatorVerifier,
		BFTValidatorSet validatorSet,
		Hasher hasher,
		HashVerifier hashVerifier
	) {
		this.accumulatorVerifier = Objects.requireNonNull(accumulatorVerifier);
		this.validatorSet = Objects.requireNonNull(validatorSet);
		this.hasher = Objects.requireNonNull(hasher);
		this.hashVerifier = Objects.requireNonNull(hashVerifier);
	}

	@Override
	public VerifiedCommandsAndProof verify(DtoCommandsAndProof commandsAndProof) throws DtoCommandsAndProofVerifierException {
		AccumulatorState start = commandsAndProof.getHead().getLedgerHeader().getAccumulatorState();
		AccumulatorState end = commandsAndProof.getTail().getLedgerHeader().getAccumulatorState();
		if (!this.accumulatorVerifier.verify(start, commandsAndProof.getCommands(), end)) {
			throw new DtoCommandsAndProofVerifierException(commandsAndProof, "Bad accumulator state");
		}

		ValidationState validationState = validatorSet.newValidationState();
		commandsAndProof.getTail().getSignatures().getSignatures().forEach((node, signature) ->
			validationState.addSignature(node, signature.timestamp(), signature.signature())
		);
		if (!validationState.complete()) {
			throw new DtoCommandsAndProofVerifierException(commandsAndProof, "Invalid signature count");
		}

		DtoLedgerHeaderAndProof endHeader = commandsAndProof.getTail();
		VoteData voteData = new VoteData(
			endHeader.getOpaque0(),
			endHeader.getOpaque1(),
			new BFTHeader(
				View.of(endHeader.getOpaque2()),
				endHeader.getOpaque3(),
				endHeader.getLedgerHeader()
			)
		);
		Map<BFTNode, TimestampedECDSASignature> signatures = endHeader.getSignatures().getSignatures();
		for (BFTNode node : signatures.keySet()) {
			TimestampedECDSASignature signature = signatures.get(node);
			final TimestampedVoteData timestampedVoteData = new TimestampedVoteData(voteData, signature.timestamp());
			final Hash voteDataHash = this.hasher.hash(timestampedVoteData);

			if (!hashVerifier.verify(node.getKey(), voteDataHash, signature.signature())) {
				throw new DtoCommandsAndProofVerifierException(commandsAndProof, "Invalid signature");
			}
		}

		// TODO: Stateful ledger header verification:
		// TODO: -verify rootHash matches
		VerifiedLedgerHeaderAndProof nextHeader = new VerifiedLedgerHeaderAndProof(
			commandsAndProof.getTail().getOpaque0(),
			commandsAndProof.getTail().getOpaque1(),
			commandsAndProof.getTail().getOpaque2(),
			commandsAndProof.getTail().getOpaque3(),
			commandsAndProof.getTail().getLedgerHeader(),
			commandsAndProof.getTail().getSignatures()
		);

		return new VerifiedCommandsAndProof(
			commandsAndProof.getCommands(),
			nextHeader
		);
	}
}
