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

package com.radixdlt.network.p2p.transport.handshake;

import com.google.common.hash.HashCode;
import com.radixdlt.DefaultSerialization;
import com.radixdlt.crypto.ECKeyOps;
import com.radixdlt.crypto.ECKeyPair;
import com.radixdlt.serialization.Serialization;
import com.radixdlt.network.p2p.transport.handshake.AuthHandshakeResult.AuthHandshakeSuccess;
import com.radixdlt.network.p2p.transport.handshake.AuthHandshakeResult.AuthHandshakeError;
import org.junit.Test;
import java.security.SecureRandom;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public final class AuthHandshakerTest {
	private final Serialization serialization = DefaultSerialization.getInstance();
	private final SecureRandom secureRandom = new SecureRandom();

	@Test
	public void test_auth_handshake() throws Exception {
		final var nodeKey1 = ECKeyPair.generateNew();
		final var nodeKey2 = ECKeyPair.generateNew();
		final var handshaker1 = new AuthHandshaker(serialization, secureRandom, ECKeyOps.fromKeyPair(nodeKey1), (byte) 0x01, HashCode.fromInt(1));
		final var handshaker2 = new AuthHandshaker(serialization, secureRandom, ECKeyOps.fromKeyPair(nodeKey2), (byte) 0x01, HashCode.fromInt(1));

		final var initMessage = handshaker1.initiate(nodeKey2.getPublicKey());
		final var handshaker2ResultPair = handshaker2.handleInitialMessage(initMessage);
		final var handshaker2Result = (AuthHandshakeSuccess) handshaker2ResultPair.getSecond();
		final var responseMessage = handshaker2ResultPair.getFirst();
		final var handshaker1Result = (AuthHandshakeSuccess) handshaker1.handleResponseMessage(responseMessage);

		assertArrayEquals(handshaker1Result.getSecrets().aes, handshaker2Result.getSecrets().aes);
		assertArrayEquals(handshaker1Result.getSecrets().mac, handshaker2Result.getSecrets().mac);
		assertArrayEquals(handshaker1Result.getSecrets().token, handshaker2Result.getSecrets().token);
	}

	@Test
	public void test_auth_handshake_fail_on_network_id_mismatch() throws Exception {
		final var nodeKey1 = ECKeyPair.generateNew();
		final var nodeKey2 = ECKeyPair.generateNew();
		final var handshaker1 = new AuthHandshaker(serialization, secureRandom, ECKeyOps.fromKeyPair(nodeKey1), (byte) 0x01, HashCode.fromInt(1));
		final var handshaker2 = new AuthHandshaker(serialization, secureRandom, ECKeyOps.fromKeyPair(nodeKey2), (byte) 0x02, HashCode.fromInt(1));

		final var initMessage = handshaker1.initiate(nodeKey2.getPublicKey());
		final var handshaker2ResultPair = handshaker2.handleInitialMessage(initMessage);
		assertTrue(handshaker2ResultPair.getSecond() instanceof AuthHandshakeError);
		assertNull(handshaker2ResultPair.getFirst());
	}
}
