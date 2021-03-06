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
import com.radixdlt.crypto.ECPublicKey;
import com.radixdlt.crypto.exception.PublicKeyException;
import com.radixdlt.serialization.DeserializeException;

import java.io.IOException;

public class ECPublicKeyDeserializer extends StdDeserializer<ECPublicKey> {
	public ECPublicKeyDeserializer() {
		super(ECPublicKey.class);
	}

	@Override
	public ECPublicKey deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
		var value = parser.getText();

		try {
			return ECPublicKey.fromHex(value);
		} catch (PublicKeyException e) {
			throw new DeserializeException("Error while parsing address " + value, e);
		}
	}
}
