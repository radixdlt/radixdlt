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

public final class ApiData {
	private final ApiDataElapsed elapsed;
	private final ApiDataCount count;

	private ApiData(ApiDataElapsed elapsed, ApiDataCount count) {
		this.elapsed = elapsed;
		this.count = count;
	}

	@JsonCreator
	public static ApiData create(
		@JsonProperty(value = "elapsed", required = true) ApiDataElapsed elapsed,
		@JsonProperty(value = "count", required = true) ApiDataCount count
	) {
		return new ApiData(elapsed, count);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (!(o instanceof ApiData)) {
			return false;
		}

		var that = (ApiData) o;
		return elapsed.equals(that.elapsed) && count.equals(that.count);
	}

	@Override
	public int hashCode() {
		return Objects.hash(elapsed, count);
	}

	public ApiDataElapsed getElapsed() {
		return elapsed;
	}

	public ApiDataCount getCount() {
		return count;
	}
}
