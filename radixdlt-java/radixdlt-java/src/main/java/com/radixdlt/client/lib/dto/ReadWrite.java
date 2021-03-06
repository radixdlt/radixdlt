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

import java.util.Objects;

public final class ReadWrite {
	private final long read;
	private final long write;

	private ReadWrite(long read, long write) {
		this.read = read;
		this.write = write;
	}

	@JsonCreator
	public static ReadWrite create(
		@JsonProperty(value = "read", required = true) long read,
		@JsonProperty(value = "write", required = true) long write
	) {
		return new ReadWrite(read, write);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (!(o instanceof ReadWrite)) {
			return false;
		}

		var that = (ReadWrite) o;
		return read == that.read && write == that.write;
	}

	@Override
	public int hashCode() {
		return Objects.hash(read, write);
	}

	@Override
	public String toString() {
		return "{read:" + read + ", write: " + write + '}';
	}

	public long getRead() {
		return read;
	}

	public long getWrite() {
		return write;
	}
}
