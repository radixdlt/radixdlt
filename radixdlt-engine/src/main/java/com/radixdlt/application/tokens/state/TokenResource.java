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

package com.radixdlt.application.tokens.state;

import com.radixdlt.constraintmachine.exceptions.AuthorizationException;
import com.radixdlt.crypto.ECPublicKey;
import com.radixdlt.identifiers.REAddr;
import com.radixdlt.utils.UInt256;

import java.util.Objects;
import java.util.Optional;

/**
 * Particle representing a fixed supply token definition
 */
public final class TokenResource implements ResourceData {
	private final REAddr addr;
	private final UInt256 granularity;
	private final boolean isMutable;
	private final ECPublicKey owner;

	public TokenResource(
		REAddr addr,
		UInt256 granularity,
		boolean isMutable,
		ECPublicKey owner
	) {
		if (!isMutable && owner != null) {
			throw new IllegalArgumentException("Can't have fixed supply and minter");
		}
		this.addr = Objects.requireNonNull(addr);
		this.granularity = granularity;
		this.isMutable = isMutable;
		this.owner = owner;
	}

	public static TokenResource createFixedSupplyResource(REAddr addr) {
		return new TokenResource(addr, UInt256.ONE, false, null);
	}

	public static TokenResource createMutableSupplyResource(REAddr addr, ECPublicKey owner) {
		return new TokenResource(addr, UInt256.ONE, true, owner);
	}

	public void verifyMintAuthorization(Optional<ECPublicKey> key) throws AuthorizationException {
		if (!key.flatMap(p -> getOwner().map(p::equals)).orElse(false)) {
			throw new AuthorizationException("Key not authorized: " + key);
		}
	}

	public void verifyBurnAuthorization(Optional<ECPublicKey> key) throws AuthorizationException {
		if (!key.flatMap(p -> getOwner().map(p::equals)).orElse(false)) {
			throw new AuthorizationException("Key not authorized: " + key);
		}
	}

	public UInt256 getGranularity() {
		return granularity;
	}

	public Optional<ECPublicKey> getOwner() {
		return Optional.ofNullable(owner);
	}

	public boolean isMutable() {
		return isMutable;
	}

	@Override
	public REAddr getAddr() {
		return addr;
	}

	@Override
	public String toString() {
		return String.format("%s{addr=%s granularity=%s isMutable=%s owner=%s}", getClass().getSimpleName(),
			this.addr, this.granularity, isMutable, owner);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof TokenResource)) {
			return false;
		}
		TokenResource that = (TokenResource) o;
		return Objects.equals(addr, that.addr)
			&& Objects.equals(this.granularity, that.granularity)
			&& this.isMutable == that.isMutable
			&& Objects.equals(owner, that.owner);
	}

	@Override
	public int hashCode() {
		return Objects.hash(addr, granularity, isMutable, owner);
	}
}
