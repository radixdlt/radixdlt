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

package com.radixdlt.consensus.bft;

import com.radixdlt.consensus.RequiresSyncConsensusEvent;
import com.radixdlt.counters.SystemCounters;
import com.radixdlt.counters.SystemCounters.CounterType;
import com.radixdlt.crypto.Hash;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import javax.annotation.Nullable;

/**
 * Synchronous queuing mechanism for consensus events which require syncing
 * before being able to effectively process it.
 *
 * A separate queue is created for each node in order to keep the message ordering invariant.
 *
 * This class is NOT thread-safe.
 */
public final class SyncQueues {
	private final Map<BFTNode, SyncQueue> queues;

	private final SystemCounters counters;

	SyncQueues(SystemCounters counters) {
		this.queues = new HashMap<>();
		this.counters = Objects.requireNonNull(counters);
	}

	class SyncQueue {
		private final LinkedList<RequiresSyncConsensusEvent> queue;

		private SyncQueue() {
			this.queue = new LinkedList<>();
		}

		/**
		 * If a vertexId is supplied, checks the top of the queue to see if
		 * the event corresponds to the vertexId. If so, returns it.
		 *
		 * TODO: cleanup interfaces
		 *
		 * @param vertexId the vertexId to check, if null, no vertexId is checked
		 * @return the top of the queue if requirements are met
		 */
		@Nullable
		public RequiresSyncConsensusEvent peek(@Nullable Hash vertexId) {
			RequiresSyncConsensusEvent e = queue.peek();

			if (e == null) {
				return null;
			}

			if (vertexId != null && !e.getQC().getProposed().getId().equals(vertexId)) {
				return null;
			}

			return e;
		}

		public void pop() {
			queue.pop();
		}

		boolean isEmptyElseAdd(RequiresSyncConsensusEvent event) {
			if (queue.isEmpty()) {
				return true;
			}

			counters.increment(CounterType.CONSENSUS_EVENTS_QUEUED_INITIAL);
			queue.addLast(event);
			return false;
		}

		public void add(RequiresSyncConsensusEvent event) {
			counters.increment(CounterType.CONSENSUS_EVENTS_QUEUED_SYNC);
			queue.addLast(event);
		}

		@Override
		public String toString() {
			return queue.toString();
		}
	}

	Collection<SyncQueue> getQueues() {
		return queues.values();
	}

	boolean isEmptyElseAdd(RequiresSyncConsensusEvent event) {
		return this.getQueue(event.getAuthor()).isEmptyElseAdd(event);
	}

	private SyncQueue getQueue(BFTNode author) {
		return queues.computeIfAbsent(author, a -> new SyncQueue());
	}

	void add(RequiresSyncConsensusEvent event) {
		queues.computeIfAbsent(event.getAuthor(), a -> new SyncQueue()).add(event);
	}

	void clear() {
		queues.clear();
	}

	@Override
	public String toString() {
		final StringJoiner joiner = new StringJoiner(",");
		queues.forEach((node, queue) -> {
			if (!queue.queue.isEmpty()) {
				joiner.add(String.format("%s=%s", node, queue));
			}
		});
		return String.format("{%s}", joiner);
	}
}
