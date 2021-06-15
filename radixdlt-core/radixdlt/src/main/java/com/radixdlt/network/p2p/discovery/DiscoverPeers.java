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

package com.radixdlt.network.p2p.discovery;

/**
 * A message acting as as trigger for PeerDiscovery to start a discovery round.
 */
public final class DiscoverPeers {

	public static DiscoverPeers create() {
		return new DiscoverPeers();
	}

	private DiscoverPeers() {
	}

	@Override
	public String toString() {
		return String.format("%s{}", this.getClass().getSimpleName());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		return o instanceof DiscoverPeers;
	}

	@Override
	public int hashCode() {
		return 1;
	}
}