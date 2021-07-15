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

public final class FixedEpochForkConfig implements ForkConfig {
	private final String name;
	private final HashCode hash;
	private final RERules reRules;
	private final long epoch;

	public FixedEpochForkConfig(
		String name,
		HashCode hash,
		RERules reRules,
		long epoch
	) {
		this.name = name;
		this.hash = hash;
		this.reRules = reRules;
		this.epoch = epoch;
	}

	public long epoch() {
		return epoch;
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
	public FixedEpochForkConfig withForksVerifier(Forks forks) {
		return new FixedEpochForkConfig(
			name,
			hash,
			reRules.withForksVerifier(hash, forks),
			epoch
		);
	}

	@Override
	public String toString() {
		return String.format("%s[%s:%s, epoch=%s]", getClass().getSimpleName(), name(), hash(), epoch);
	}
}
