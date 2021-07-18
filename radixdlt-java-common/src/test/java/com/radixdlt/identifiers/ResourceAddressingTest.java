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

package com.radixdlt.identifiers;

import org.junit.Test;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.radixdlt.crypto.ECKeyPair;
import com.radixdlt.crypto.ECPublicKey;
import com.radixdlt.utils.Bytes;
import com.radixdlt.utils.Pair;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ResourceAddressingTest {
	private final ResourceAddressing resourceAddressing = ResourceAddressing.bech32("_rb");
	private final BiMap<Pair<String, String>, String> reAddressToRri = HashBiMap.create(
		Map.of(
			Pair.of("xrd", "01"), "xrd_rb1qya85pwq",
			Pair.of("usdc", "03" + "00".repeat(26)), "usdc_rb1qvqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq6gwwwd"
		)
	);

	private final Map<String, String> invalidRris = Map.of(
		"xrd1pzdsczc", "no _rb suffix",
		"xrd_rb1avu205I", "invalid address type (0)",
		"usdc_rb1qg8vs72e", "invalid address type (2)",
		"usdc_rb1qqqsqs6ztc", "invalid length for address type 1",
		"usdc_rb1qvgxjc9r", "invalid length for address type 3",
		"xrd_2_rb1qvpsxqcrqvpsxqcrqvpsxqcrqvpsxqcrqvpsxqcrqvpszyaqyw", "invalid characters in hrp"
	);

	private final BiMap<Pair<Integer, String>, String> privateKeyAndNameToRri = HashBiMap.create(
		Map.of(
			Pair.of(1, "foo"), "foo_rb1qv9ee5j4qun9frqj2mcg79maqq55n46u5ypn2j0g9c3q32j6y3",
			Pair.of(1, "bar"), "bar_rb1qwaa87cznx0nmeq08dya2ae43u92g4g0nkfktd9u9lpq6hgjca",
			Pair.of(2, "foo"), "foo_rb1qvmf6ak360gxjfhxeh0x5tn99gjzzh5d7u3kvktj26rsu5qa3u",
			Pair.of(2, "bar"), "bar_rb1qd3t7gnvwxddj2wxg5dl4adr7er9uw62g7x0ku6hyw4qfk0pfz"
		)
	);

	private final Map<String, String> systemRris = Map.of(
		"xrd", "xrd_rb1qya85pwq",
		"eth", "eth_rb1qynl40gy",
		"btc", "btc_rb1qytls7qn"
	);

	@Test
	public void test_rri_serialization() {
		reAddressToRri.forEach((pair, expected) -> {
			var reAddr = REAddr.of(Bytes.fromHexString(pair.getSecond()));
			var rri = resourceAddressing.of(pair.getFirst(), reAddr);
			assertThat(expected).isEqualTo(rri);
		});
	}

	@Test
	public void test_rri_deserialization() {
		reAddressToRri.forEach((expected, rri) -> {
			var pair = resourceAddressing.parse(rri);
			var expectedAddr = REAddr.of(Bytes.fromHexString(expected.getSecond()));

			pair.map((symbol, address) -> {
				assertThat(expected.getFirst()).isEqualTo(symbol);
				assertThat(expectedAddr).isEqualTo(address);
				return null;
			});
		});
	}

	@Test
	public void test_invalid_rris() {
		for (var e : invalidRris.entrySet()) {
			assertThatThrownBy(() -> resourceAddressing.parse(e.getKey()), e.getValue()).isInstanceOf(IllegalArgumentException.class);
		}
	}

	private ECPublicKey publicKeyOfPrivateKey(int privateKeyScalar) {
		assertThat(privateKeyScalar).isLessThanOrEqualTo(9);
		try {
			return ECKeyPair.fromPrivateKey(Bytes.fromHexString("0".repeat(63) + privateKeyScalar)).getPublicKey();
		} catch (Exception e) {
			throw new IllegalStateException("bad key");
		}
	}

	private String rriFromPKAndName(int privateKey, String name) {
		var reAddr = REAddr.ofHashedKey(publicKeyOfPrivateKey(privateKey), name);
		return resourceAddressing.of(name, reAddr);
	}

	@Test
	public void test_system_rris() {
		var systemTokenREAddr = REAddr.ofNativeToken();
		for (var e : systemRris.entrySet()) {
			var rri = resourceAddressing.of(e.getKey(), systemTokenREAddr);
			assertThat(rri).isEqualTo(e.getValue());
		}
	}
	@Test
	public void test_rri_from_pubkey_serialization() {
		privateKeyAndNameToRri.forEach((pair, expected) -> {
			var rri = rriFromPKAndName(pair.getFirst(), pair.getSecond());
			assertThat(rri).isEqualTo(expected);
		});
	}
}