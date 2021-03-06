/*
 * (C) Copyright 2020 Radix DLT Ltd
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
package com.radixdlt.mempool;

import com.google.common.collect.Lists;
import com.radixdlt.atom.Txn;
import com.radixdlt.counters.SystemCounters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Simple mempool which performs no validation and removes on commit.
 */
public final class SimpleMempool implements Mempool<Txn> {
	private final Set<Txn> data = new HashSet<>();
	private final SystemCounters counters;
	private final Random random;
	private final int maxSize;

	public SimpleMempool(
		SystemCounters counters,
		int maxSize,
		Random random
	) {
		if (maxSize <= 0) {
			throw new IllegalArgumentException("mempool.maxSize must be positive: " + maxSize);
		}
		this.counters = Objects.requireNonNull(counters);
		this.maxSize = maxSize;
		this.random = Objects.requireNonNull(random);
	}

	@Override
	public void add(Txn txn) throws MempoolFullException, MempoolDuplicateException {
		if (this.data.size() >= maxSize) {
			throw new MempoolFullException(
				String.format("Mempool full: %s of %s items", this.data.size(), maxSize)
			);
		}
		if (!this.data.add(txn)) {
			throw new MempoolDuplicateException(String.format("Mempool already has command %s", txn));
		}

		updateCounts();
	}

	@Override
	public List<Txn> committed(List<Txn> commands) {
		commands.forEach(this.data::remove);
		updateCounts();
		return List.of();
	}

	@Override
	public int getCount() {
		return data.size();
	}

	@Override
	public List<Txn> getTxns(int count, List<Txn> seen) {
		int size = Math.min(count, this.data.size());
		if (size > 0) {
			List<Txn> commands = Lists.newArrayList();
			var values = new ArrayList<>(this.data);
			Collections.shuffle(values, random);

			Iterator<Txn> i = values.iterator();
			while (commands.size() < size && i.hasNext()) {
				var a = i.next();
				if (!seen.contains(a)) {
					commands.add(a);
				}
			}
			return commands;
		} else {
			return Collections.emptyList();
		}
	}

	@Override
	public List<Txn> scanUpdateAndGet(Predicate<MempoolMetadata> predicate, Consumer<MempoolMetadata> operator) {
		return List.of();
	}

	private void updateCounts() {
		this.counters.set(SystemCounters.CounterType.MEMPOOL_COUNT, this.data.size());
		this.counters.set(SystemCounters.CounterType.MEMPOOL_MAXCOUNT, maxSize);
	}

	@Override
	public String toString() {
		return String.format("%s[%x:%s/%s]",
			getClass().getSimpleName(), System.identityHashCode(this), this.data.size(), maxSize);
	}
}
