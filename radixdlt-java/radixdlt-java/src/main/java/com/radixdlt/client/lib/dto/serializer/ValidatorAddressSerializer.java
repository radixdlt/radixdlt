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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.radixdlt.client.lib.api.ValidatorAddress;
import com.radixdlt.identifiers.ValidatorAddressing;
import com.radixdlt.networks.Addressing;

import java.io.IOException;

public class ValidatorAddressSerializer extends StdSerializer<ValidatorAddress> {
	private final ValidatorAddressing addressing;

	public ValidatorAddressSerializer(int networkId) {
		super(ValidatorAddress.class);
		addressing = Addressing.ofNetworkId(networkId).forValidators();
	}

	@Override
	public void serialize(ValidatorAddress value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		gen.writeString(addressing.of(value.getAddress()));
	}
}
