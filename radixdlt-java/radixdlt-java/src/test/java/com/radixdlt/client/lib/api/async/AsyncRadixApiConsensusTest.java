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
package com.radixdlt.client.lib.api.async;

import org.junit.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AsyncRadixApiConsensusTest {
	private static final String BASE_URL = "http://localhost/";

	private static final String NETWORK_ID = "{\"result\":{\"networkId\":99},\"id\":\"1\",\"jsonrpc\":\"2.0\"}";
	private static final String CONFIGURATION = "{\"result\":{\"pacemakerTimeout\":3000,\"bftSyncPatienceMs\":200},"
		+ "\"id\":\"2\",\"jsonrpc\":\"2.0\"}\n";
	private static final String DATA = "{\"result\":{\"timeoutQuorums\":1,\"rejected\":0,\"voteQuorums\":79981,"
		+ "\"vertexStoreForks\":1,\"sync\":{\"requestsSent\":0,\"requestTimeouts\":0},\"timeout\":1,\"stateVers"
		+ "ion\":159960,\"processed\":159959,\"timedOutViews\":1,\"vertexStoreSize\":2,\"vertexStoreRebuilds\":"
		+ "0,\"consensusEvents\":319926,\"indirectParent\":1,\"proposalsMade\":79981},\"id\":\"2\",\"jsonrpc\":"
		+ "\"2.0\"}\n";

	private final HttpClient client = mock(HttpClient.class);

	@Test
	public void testConfiguration() throws IOException {
		prepareClient(CONFIGURATION)
			.map(RadixApi::withTrace)
			.join()
			.onFailure(failure -> fail(failure.toString()))
			.onSuccess(client -> client.consensus().configuration().join()
				.onFailure(failure -> fail(failure.toString()))
				.onSuccess(configuration -> assertEquals(200L, configuration.getBftSyncPatienceMs()))
				.onSuccess(configuration -> assertEquals(3000L, configuration.getPacemakerTimeout())));
	}

	@Test
	public void testData() throws IOException {
		prepareClient(DATA)
			.map(RadixApi::withTrace)
			.join()
			.onFailure(failure -> fail(failure.toString()))
			.onSuccess(
				client -> client.consensus().data().join()
					.onFailure(failure -> fail(failure.toString()))
					.onSuccess(data -> assertEquals(159959L, data.getProcessed()))
					.onSuccess(data -> assertEquals(159960L, data.getStateVersion())));
	}

	private Promise<RadixApi> prepareClient(String responseBody) throws IOException {
		@SuppressWarnings("unchecked")
		var response = (HttpResponse<String>) mock(HttpResponse.class);
		var completableFuture = new CompletableFuture<HttpResponse<String>>();

		when(response.body()).thenReturn(NETWORK_ID, responseBody);
		when(client.<String>sendAsync(any(), any())).thenReturn(completableFuture);

		completableFuture.completeAsync(() -> response);
		return AsyncRadixApi.connect(BASE_URL, RadixApi.DEFAULT_PRIMARY_PORT, RadixApi.DEFAULT_SECONDARY_PORT, client);
	}
}
