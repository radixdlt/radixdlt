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

package com.radixdlt.statecomputer.forks;

import com.google.common.hash.HashCode;

public final class CandidateForkConfig implements ForkConfig {
	private final String name;
	private final HashCode hash;
	private final RERules reRules;
	private int requiredStake;
	private long minEpoch;

	public CandidateForkConfig(
		String name,
		HashCode hash,
		RERules reRules,
		int requiredStake,
		long minEpoch
	) {
		this.name = name;
		this.hash = hash;
		this.reRules = reRules;
		this.requiredStake = requiredStake;
		this.minEpoch = minEpoch;
	}

	public long minEpoch() {
		return minEpoch;
	}

	public int requiredStake() {
		return requiredStake;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public HashCode hash() {
		return hash;
	}

	@Override
	public RERules engineRules() {
		return reRules;
	}

	@Override
	public CandidateForkConfig withForksVerifier(Forks forks) {
		return new CandidateForkConfig(
			name,
			hash,
			reRules.withForksVerifier(hash, forks),
			requiredStake,
			minEpoch
		);
	}

	@Override
	public String toString() {
		return String.format("%s[%s:%s, min_epoch=%s, required_stake=%s]",
			getClass().getSimpleName(), name(), hash(), minEpoch, requiredStake);
	}
}
