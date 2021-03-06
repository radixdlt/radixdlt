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

package com.radixdlt.client.lib.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.radixdlt.identifiers.AID;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

public final class TxDTO {
	private final AID txId;

	private TxDTO(AID txId) {
		this.txId = txId;
	}

	@JsonCreator
	public static TxDTO create(@JsonProperty(value = "txID", required = true) AID txId) {
		requireNonNull(txId);

		return new TxDTO(txId);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (!(o instanceof TxDTO)) {
			return false;
		}

		var txDTO = (TxDTO) o;
		return txId.equals(txDTO.txId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(txId);
	}

	@Override
	public String toString() {
		return "{" + txId.toJson() + '}';
	}

	public AID getTxId() {
		return txId;
	}
}
