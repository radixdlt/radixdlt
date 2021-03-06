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

package com.radixdlt.client.lib.dto.serializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.radixdlt.client.lib.api.ValidatorAddress;
import com.radixdlt.identifiers.ValidatorAddressing;
import com.radixdlt.networks.Addressing;
import com.radixdlt.serialization.DeserializeException;

import java.io.IOException;

public class ValidatorAddressDeserializer extends StdDeserializer<ValidatorAddress> {
	private final ValidatorAddressing addressing;

	public ValidatorAddressDeserializer(int networkId) {
		super(ValidatorAddress.class);
		addressing = Addressing.ofNetworkId(networkId).forValidators();
	}

	@Override
	public ValidatorAddress deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
		var value = parser.getText();

		try {
			return ValidatorAddress.of(addressing.parse(value));
		} catch (IllegalArgumentException e) {
			throw new DeserializeException("Error while parsing address " + value, e);
		}
	}
}
