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

package com.radixdlt.environment.deterministic;

import com.radixdlt.consensus.bft.BFTNode;
import com.radixdlt.environment.RemoteEventProcessorOnRunner;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class DeterministicMempoolProcessor implements DeterministicMessageProcessor {
	private final Map<Class<?>, List<RemoteEventProcessorOnRunner<?>>> runners;

	@Inject
	public DeterministicMempoolProcessor(
		Set<RemoteEventProcessorOnRunner<?>> remoteEventProcessorOnRunners
	) {
	    this.runners = remoteEventProcessorOnRunners.stream()
			.collect(Collectors.<RemoteEventProcessorOnRunner<?>, Class<?>>groupingBy(RemoteEventProcessorOnRunner::getEventClass));
	}

	@Override
	public void start() {
		// No-op
	}

	private <T> void execute(BFTNode sender, T event) {
		Class<T> eventClass = (Class<T>) event.getClass();
		List<RemoteEventProcessorOnRunner<?>> eventRunners = runners.get(eventClass);
		if (eventRunners.isEmpty()) {
			throw new IllegalStateException("Unknown message type: " + event.getClass());
		}
		eventRunners.forEach(p -> p.getProcessor(eventClass).ifPresent(r -> r.process(sender, event)));
	}

	@Override
	public void handleMessage(BFTNode origin, Object message) {
	    this.execute(origin, message);
	}
}
