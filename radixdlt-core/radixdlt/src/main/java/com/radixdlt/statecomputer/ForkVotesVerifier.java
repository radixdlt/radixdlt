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

package com.radixdlt.statecomputer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.common.hash.HashCode;
import com.radixdlt.application.validators.state.ValidatorSystemMetadata;
import com.radixdlt.atom.SubstateTypeId;
import com.radixdlt.constraintmachine.REProcessedTxn;
import com.radixdlt.constraintmachine.RawSubstateBytes;
import com.radixdlt.constraintmachine.SubstateDeserialization;
import com.radixdlt.constraintmachine.SubstateIndex;
import com.radixdlt.crypto.ECPublicKey;
import com.radixdlt.crypto.exception.PublicKeyException;
import com.radixdlt.engine.BatchVerifier;
import com.radixdlt.engine.MetadataException;
import com.radixdlt.serialization.DeserializeException;
import com.radixdlt.statecomputer.forks.ForkConfig;
import com.radixdlt.statecomputer.forks.Forks;
import com.radixdlt.store.EngineStore;

import java.util.Arrays;
import java.util.List;

public final class ForkVotesVerifier implements BatchVerifier<LedgerAndBFTProof> {

	private final BatchVerifier<LedgerAndBFTProof> baseVerifier;
	private final HashCode curForkHash;
	private final Forks forks;

	public ForkVotesVerifier(
		BatchVerifier<LedgerAndBFTProof> baseVerifier,
	 	HashCode curForkHash,
	 	Forks forks
	) {
		this.baseVerifier = baseVerifier;
		this.curForkHash = curForkHash;
		this.forks = forks;
	}

	@Override
	public LedgerAndBFTProof processMetadata(
		LedgerAndBFTProof metadata,
		EngineStore<LedgerAndBFTProof> engineStore,
		List<REProcessedTxn> txns
	) throws MetadataException {
		final var ignoredMetadata = baseVerifier.processMetadata(metadata, engineStore, txns);
		// just a sanity check, otherwise it would be silently ignored
		if (ignoredMetadata != metadata) {
			throw new IllegalStateException("Unexpected metadata modification by the baseVerifier");
		}

		// no forks checking if not end of epoch
		if (metadata.getProof().getNextValidatorSet().isEmpty()) {
			return metadata;
		}

		// add next validators system metadata substates
		final var metadataWithValidators = metadata
			.withValidatorsSystemMetadata(getValidatorsSystemMetadata(engineStore, metadata));

		// add next fork hash, if needed
		return forks.findNextForkConfig(curForkHash, metadataWithValidators)
			.map(nextForkConfig -> metadataWithValidators.withNextForkHash(nextForkConfig.hash()))
			.orElse(metadataWithValidators);
	}

	private ImmutableList<RawSubstateBytes> getValidatorsSystemMetadata(
		EngineStore<LedgerAndBFTProof> engineStore,
		LedgerAndBFTProof ledgerAndBFTProof
	) {
		final var validatorSet = ledgerAndBFTProof.getProof().getNextValidatorSet().orElseThrow();

		try (var validatorMetadataCursor = engineStore.openIndexedCursor(
			SubstateIndex.create(SubstateTypeId.VALIDATOR_SYSTEM_META_DATA.id(), ValidatorSystemMetadata.class))
		) {
			return Streams.stream(validatorMetadataCursor)
				.filter(rawSubstate -> {
					var keyBytes = Arrays.copyOfRange(rawSubstate.getData(), 2, 2 + ECPublicKey.COMPRESSED_BYTES);
					try {
						var key = ECPublicKey.fromBytes(keyBytes);
						return validatorSet.containsNode(key);
					} catch (PublicKeyException ex) {
						throw new RuntimeException(ex);
					}
				})
				.collect(ImmutableList.toImmutableList());
		}
	}
}
