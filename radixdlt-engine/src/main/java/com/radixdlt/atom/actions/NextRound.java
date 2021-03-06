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

package com.radixdlt.atom.actions;

import com.radixdlt.atom.TxAction;
import com.radixdlt.crypto.ECPublicKey;

import java.util.function.LongFunction;

public final class NextRound implements TxAction {
	private final long view;
	private final boolean isTimeout;
	private final long timestamp;
	private final LongFunction<ECPublicKey> leaderMapping;

	public NextRound(long view, boolean isTimeout, long timestamp, LongFunction<ECPublicKey> leaderMapping) {
		this.view = view;
		this.isTimeout = isTimeout;
		this.timestamp = timestamp;
		this.leaderMapping = leaderMapping;
	}

	public boolean isTimeout() {
		return isTimeout;
	}

	public long view() {
		return view;
	}

	public long timestamp() {
		return timestamp;
	}

	public LongFunction<ECPublicKey> leaderMapping() {
		return leaderMapping;
	}

	@Override
	public String toString() {
		return String.format("%s{view=%s}", this.getClass().getSimpleName(), view);
	}
}
