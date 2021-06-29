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

package com.radixdlt.store;

import com.google.common.hash.HashCode;
import com.radixdlt.atom.Substate;
import com.radixdlt.atom.CloseableCursor;
import com.radixdlt.atom.SubstateId;
import com.radixdlt.atom.SubstateStore;
import com.radixdlt.atom.Txn;
import com.radixdlt.application.tokens.state.TokenResource;
import com.radixdlt.constraintmachine.ShutdownAllIndex;
import com.radixdlt.constraintmachine.REStateUpdate;
import com.radixdlt.constraintmachine.Particle;
import com.radixdlt.constraintmachine.REOp;
import com.radixdlt.constraintmachine.RawSubstateBytes;
import com.radixdlt.constraintmachine.SubstateDeserialization;
import com.radixdlt.identifiers.REAddr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

public final class InMemoryEngineStore<M> implements EngineStore<M>, SubstateStore {
	private final Object lock = new Object();
	private final Map<SubstateId, REStateUpdate> storedParticles = new HashMap<>();
	private final Map<REAddr, Particle> addrParticles = new HashMap<>();
	private Optional<HashCode> currentForkHash = Optional.empty();

	@Override
	public void storeTxn(Transaction dbTxn, Txn txn, List<REStateUpdate> stateUpdates) {
		synchronized (lock) {
			stateUpdates.forEach(i -> storedParticles.put(i.getSubstate().getId(), i));
			stateUpdates.stream()
				.filter(REStateUpdate::isBootUp)
				.map(REStateUpdate::getRawSubstate)
				.forEach(p -> {
					// FIXME: Superhack
					if (p instanceof TokenResource) {
						var tokenDef = (TokenResource) p;
						addrParticles.put(tokenDef.getAddr(), p);
					}
				});
		}
	}

	@Override
	public void storeMetadata(Transaction txn, M metadata) {
		 // No-op
	}

	@Override
	public void storeCurrentForkHash(Transaction txn, HashCode forkHash) {
		synchronized (lock) {
			this.currentForkHash = Optional.of(forkHash);
		}
	}

	@Override
	public Optional<HashCode> getCurrentForkHash() {
		return this.currentForkHash;
	}

	@Override
	public <V> V reduceUpParticles(
		V initial,
		BiFunction<V, Particle, V> outputReducer,
		SubstateDeserialization substateDeserialization,
		Class<? extends Particle>... particleClass
	) {
		V v = initial;
		var types = Set.of(particleClass);

		synchronized (lock) {
			for (var i : storedParticles.values()) {
				if (!i.isBootUp() || !isOneOf(types, i.getRawSubstate())) {
					continue;
				}

				v = outputReducer.apply(v, i.getRawSubstate());
			}
		}
		return v;
	}

	private static boolean isOneOf(Set<Class<? extends Particle>> bundle, Particle instance) {
		return bundle.stream().anyMatch(v -> v.isInstance(instance));
	}

	@Override
	public CloseableCursor<RawSubstateBytes> openIndexedCursor(Transaction dbTxn, ShutdownAllIndex index) {
		final List<RawSubstateBytes> substates = new ArrayList<>();
		synchronized (lock) {
			for (var i : storedParticles.values()) {
				if (!i.isBootUp()) {
					continue;
				}
				if (!index.test(i.getRawSubstateBytes())) {
					continue;
				}
				substates.add(i.getRawSubstateBytes());
			}
		}

		return CloseableCursor.wrapIterator(substates.iterator());
	}

	@Override
	public CloseableCursor<Substate> openIndexedCursor(Class<? extends Particle> substateClass, SubstateDeserialization deserialization) {
		final List<Substate> substates = new ArrayList<>();
		synchronized (lock) {
			for (var i : storedParticles.values()) {
				if (!i.isBootUp() || !substateClass.isInstance(i.getRawSubstate())) {
					continue;
				}
				substates.add(i.getSubstate());
			}
		}

		return CloseableCursor.wrapIterator(substates.iterator());
	}

	@Override
	public Transaction createTransaction() {
		return new Transaction() { };
	}

	@Override
	public boolean isVirtualDown(Transaction txn, SubstateId substateId) {
		synchronized (lock) {
			var inst = storedParticles.get(substateId);
			return inst != null && inst.isShutDown();
		}
	}

	public Optional<REOp> getSpin(SubstateId substateId) {
		synchronized (lock) {
			var inst = storedParticles.get(substateId);
			return Optional.ofNullable(inst).map(REStateUpdate::getOp);
		}
	}

	@Override
	public Optional<Particle> loadUpParticle(Transaction txn, SubstateId substateId, SubstateDeserialization deserialization) {
		synchronized (lock) {
			var inst = storedParticles.get(substateId);
			if (inst == null || inst.getOp() != REOp.UP) {
				return Optional.empty();
			}

			var particle = inst.getRawSubstate();
			return Optional.of(particle);
		}
	}

	@Override
	public Optional<Particle> loadAddr(Transaction dbTxn, REAddr rri, SubstateDeserialization deserialization) {
		synchronized (lock) {
			return Optional.ofNullable(addrParticles.get(rri));
		}
	}
}
