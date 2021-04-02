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

package com.radixdlt.client.store;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.inject.Inject;
import com.radixdlt.atommodel.tokens.FixedSupplyTokenDefinitionParticle;
import com.radixdlt.atommodel.tokens.MutableSupplyTokenDefinitionParticle;
import com.radixdlt.atommodel.tokens.StakedTokensParticle;
import com.radixdlt.atommodel.tokens.TransferrableTokensParticle;
import com.radixdlt.atommodel.tokens.UnallocatedTokensParticle;
import com.radixdlt.atommodel.validators.RegisteredValidatorParticle;
import com.radixdlt.atommodel.validators.UnregisteredValidatorParticle;
import com.radixdlt.atomos.RRIParticle;
import com.radixdlt.constraintmachine.ParsedInstruction;
import com.radixdlt.constraintmachine.Particle;
import com.radixdlt.constraintmachine.Spin;
import com.radixdlt.identifiers.AID;
import com.radixdlt.identifiers.RadixAddress;
import com.radixdlt.serialization.Serialization;
import com.radixdlt.store.berkeley.FullTransaction;
import com.radixdlt.utils.UInt256;
import com.radixdlt.utils.functional.Failure;
import com.radixdlt.utils.functional.Result;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.radixdlt.serialization.SerializationUtils.restore;

//TODO: error handling!!!!
public class TransactionParser {
	private static final Logger log = LogManager.getLogger();

	private final Serialization serialization;

	@Inject
	public TransactionParser(Serialization serialization) {
		this.serialization = serialization;
	}

	public Result<TxHistoryEntry> parse(RadixAddress owner, FullTransaction txWithId, Instant txDate) {
		var instructions = txWithId.getTx()
			.uniqueInstructions()
			.map(i -> restore(serialization, i.getData(), Particle.class)
				.map(substate -> ParsedInstruction.of(substate, i.getNextSpin())))
			.peek(instruction -> instruction.onFailure(this::reportError))
			.filter(Result::isSuccess)
			.map(p -> p.fold(this::shouldNeverHappen, v -> v))
			.collect(Collectors.toList());

		return new ParsingContext(instructions, txWithId.getTx().getMessage(), txWithId.getTxId(), txDate, owner)
			.parse();
	}

	private static class ParsingContext {
		private final List<ParsedInstruction> input;
		private final String message;
		private final AID txId;
		private final Instant txDate;
		private final RadixAddress owner;
		private final List<ActionEntry> actions = new ArrayList<>();

		private int pos;
		private UInt256 fee = UInt256.ZERO;

		ParsingContext(List<ParsedInstruction> input, String message, AID txId, Instant txDate, RadixAddress owner) {
			this.input = input;
			this.message = message;
			this.txId = txId;
			this.txDate = txDate;
			this.owner = owner;
		}

		public Result<TxHistoryEntry> parse() {
			parseActions();

			return Result.ok(TxHistoryEntry.create(
				txId,
				txDate,
				fee,
				//TODO: add support for encrypted messages
				MessageEntry.fromPlainString(message).orElse(null),
				actions
			));
		}

		private void parseActions() {
			while (pos < input.size()) {
				if (parseStake()) {
					continue;
				}
				if (parseUnStake()) {
					continue;
				}
				if (parseTransfer()) {
					continue;
				}

				if (parseTokenDefinition()) {
					continue;
				}
				if (parseRegisterValidator()) {
					continue;
				}
				if (parseUnregisterValidator()) {
					continue;
				}
				if (parseMint()) {
					continue;
				}
				if (parseFeeBurn()) {
					continue;
				}
			}
		}

		private boolean parseStake() {
			if (current() instanceof StakedTokensParticle && isUp()) {
				var stake = (StakedTokensParticle) current();
				pos++;

				if (current() instanceof TransferrableTokensParticle && isDown()) {
					pos++;
				}

				if (current() instanceof TransferrableTokensParticle && isUp()) {
					// remainder transfer, optional
					pos++;
				}

				actions.add(ActionEntry.fromStake(stake));

				return true;
			}
			return false;
		}

		private boolean parseUnStake() {
			if (current() instanceof TransferrableTokensParticle) {
				pos++;

				if (!(current() instanceof StakedTokensParticle)) {
					pos--;
					return false;
				}

				var unstake = (StakedTokensParticle) current();
				pos++;

				if (current() instanceof StakedTokensParticle && isUp()) {
					// remainder transfer, optional
					pos++;
				}

				actions.add(ActionEntry.fromUnstake(unstake));

				return true;
			}
			return false;
		}

		private boolean parseTransfer() {
			if (current() instanceof TransferrableTokensParticle && isUp()) {
				var transfer = (TransferrableTokensParticle) current();
				pos++;

				if (current() instanceof TransferrableTokensParticle && isDown()) {
					pos++;
				}

				if (current() instanceof TransferrableTokensParticle && isUp()) {
					//This can be remainder or beginning if the next transfer/unstake
					var remainder = (TransferrableTokensParticle) current();

					// Transfer to self is the beginning of unstake
					if (!remainder.getAddress().equals(owner)) {
						pos++;
					}
				}

				actions.add(ActionEntry.transfer(transfer, owner));
			}
			return false;
		}

		private boolean parseTokenDefinition() {
			if (!(current() instanceof RRIParticle)) {
				return false;
			}

			pos++;

			if (current() instanceof UnallocatedTokensParticle) {
				pos++;
			}

			if (current() instanceof MutableSupplyTokenDefinitionParticle ||
				current() instanceof FixedSupplyTokenDefinitionParticle) {
				pos++;
			}
			return true;
		}

		private boolean parseRegisterValidator() {
			if (current() instanceof UnregisteredValidatorParticle && isUp()) {
				pos++;

				if(current() instanceof RegisteredValidatorParticle && isDown()) {
					pos++;
				}
				return true;
			}
			return false;
		}

		private boolean parseUnregisterValidator() {
			if (current() instanceof RegisteredValidatorParticle && isDown()) {
				pos++;

				if(current() instanceof RegisteredValidatorParticle && isUp()) {
					pos++;
				}
				return true;
			}
			return false;
		}

		private boolean parseMint() {
			if (current() instanceof TransferrableTokensParticle) {
				pos++;

				if (!(current() instanceof UnallocatedTokensParticle)) {
					pos--;
					return false;
				}

				pos++;

				if (current() instanceof UnallocatedTokensParticle) {
					// remainder transfer, optional
					pos++;
				}
			}
			return false;
		}

		private boolean parseFeeBurn() {
			if (current() instanceof UnallocatedTokensParticle) {
				var feeParticle = (UnallocatedTokensParticle) current();
				pos++;

				if (current() instanceof TransferrableTokensParticle && isDown()) {
					pos++;
				}

				if (current() instanceof TransferrableTokensParticle && isUp()) {
					//This can be remainder or beginning if the next transfer/unstake
					var remainder = (TransferrableTokensParticle) current();

					// Transfer to self is the transfer of remainder
					if (remainder.getAddress().equals(owner)) {
						pos++;
					}
				}

				fee = fee.add(feeParticle.getAmount());

				return true;
			}
			return false;
		}

		private Particle current() {
			return input.get(pos).getParticle();
		}

		private boolean isUp() {
			return input.get(pos).getSpin() == Spin.UP;
		}

		private boolean isDown() {
			return input.get(pos).getSpin() == Spin.DOWN;
		}
	}

	private void reportError(Failure failure) {
		log.error(failure.message());
	}

	private <T> T shouldNeverHappen(Failure f) {
		log.error("Should never happen {}", f.message());
		return null;
	}
}