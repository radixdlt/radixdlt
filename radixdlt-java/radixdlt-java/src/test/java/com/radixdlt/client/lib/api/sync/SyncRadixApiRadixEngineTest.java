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
package com.radixdlt.client.lib.api.sync;

import org.junit.Test;

import com.radixdlt.utils.functional.Result;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SyncRadixApiRadixEngineTest {
	private static final String BASE_URL = "http://localhost/";

	private static final String NETWORK_ID = "{\"result\":{\"networkId\":99},\"id\":\"1\",\"jsonrpc\":\"2.0\"}";
	private static final String CONFIGURATION = "{\"result\":[{\"maxSigsPerRound\":50,\"maxValidators\":100,"
		+ "\"name\":\"olympia-first-epoch\",\"maxRounds\":1500000,\"epoch\":0,\"version\":\"olympia_v1\"},"
		+ "{\"maxSigsPerRound\":50,\"maxValidators\":100,\"name\":\"olympia\",\"maxRounds\":10000,\"epoch\":2,"
		+ "\"version\":\"olympia_v1\"}],\"id\":\"2\",\"jsonrpc\":\"2.0\"}\n";
	private static final String DATA = "{\"result\":{\"systemTransactions\":37884,\"invalidProposedCommands\":1,"
		+ "\"userTransactions\":2016},\"id\":\"2\",\"jsonrpc\":\"2.0\"}\n";

	private final HttpClient client = mock(HttpClient.class);

	@Test
	public void testConfiguration() throws Exception {
		prepareClient(CONFIGURATION)
				.map(RadixApi::withTrace)
				.onFailure(failure -> fail(failure.toString()))
				.onSuccess(client -> client.radixEngine().configuration()
						.onFailure(failure -> fail(failure.toString()))
						.onSuccess(configuration -> assertEquals(2, configuration.size()))
						.onSuccess(configuration -> assertEquals("olympia", configuration.get(1).getName())));
	}

	@Test
	public void testData() throws Exception {
		prepareClient(DATA)
			.map(RadixApi::withTrace)
			.onFailure(failure -> fail(failure.toString()))
			.onSuccess(
				client -> client.radixEngine().data()
					.onFailure(failure -> fail(failure.toString()))
					.onSuccess(data -> assertEquals(37884L, data.getSystemTransactions()))
					.onSuccess(data -> assertEquals(2016L, data.getUserTransactions()))
					.onSuccess(data -> assertEquals(1L, data.getInvalidProposedCommands())));
	}

	private Result<RadixApi> prepareClient(String responseBody) throws Exception {
		@SuppressWarnings("unchecked")
		var response = (HttpResponse<String>) mock(HttpResponse.class);

		when(response.body()).thenReturn(NETWORK_ID, responseBody);
		when(client.<String>send(any(), any())).thenReturn(response);

		return SyncRadixApi.connect(BASE_URL, RadixApi.DEFAULT_PRIMARY_PORT, RadixApi.DEFAULT_SECONDARY_PORT, client);
	}
}
