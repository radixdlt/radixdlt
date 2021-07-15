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

package com.radixdlt.api.controller;

import com.google.common.collect.ImmutableMap;
import com.radixdlt.api.service.ForkVoteStatusService;
import static com.radixdlt.api.service.ForkVoteStatusService.ForkVoteStatus.NO_ACTION_NEEDED;
import static com.radixdlt.api.service.ForkVoteStatusService.ForkVoteStatus.VOTE_REQUIRED;

import com.radixdlt.api.service.PeersForksHashesInfoService;
import com.radixdlt.statecomputer.forks.ForksEpochStore;
import org.json.JSONObject;
import org.junit.Test;

import com.radixdlt.api.service.NetworkInfoService;

import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.RoutingHandler;
import io.undertow.util.HeaderMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.radixdlt.api.data.NodeStatus.BOOTING;
import static com.radixdlt.api.data.NodeStatus.STALLED;
import static com.radixdlt.api.data.NodeStatus.SYNCING;
import static com.radixdlt.api.data.NodeStatus.UP;

public class HealthControllerTest {
	private final NetworkInfoService networkInfoService = mock(NetworkInfoService.class);
	private final ForkVoteStatusService forkVoteStatusService = mock(ForkVoteStatusService.class);
	private final PeersForksHashesInfoService peersForksHashesInfoService = mock(PeersForksHashesInfoService.class);
	private final ForksEpochStore forksEpochStore = mock(ForksEpochStore.class);
	private final HealthController controller = new HealthController(
		networkInfoService, forkVoteStatusService, peersForksHashesInfoService, forksEpochStore);

	@Test
	public void routesAreConfigured() {
		var handler = mock(RoutingHandler.class);

		controller.configureRoutes("/health", handler);

		verify(handler).get(eq("/health"), any());
	}

	@Test
	public void healthStatusIsReturned() {
		var exchange = mock(HttpServerExchange.class);
		var sender = mock(Sender.class);

		when(exchange.getResponseHeaders()).thenReturn(new HeaderMap());
		when(exchange.getResponseSender()).thenReturn(sender);
		when(networkInfoService.nodeStatus()).thenReturn(BOOTING, SYNCING, UP, STALLED);
		when(forkVoteStatusService.forkVoteStatus()).thenReturn(VOTE_REQUIRED, NO_ACTION_NEEDED, VOTE_REQUIRED, NO_ACTION_NEEDED);
		when(forkVoteStatusService.currentFork()).thenReturn(
			new JSONObject().put("name", "fork1"),
			new JSONObject().put("name", "fork2"),
			new JSONObject().put("name", "fork3"),
			new JSONObject().put("name", "fork4")
		);
		when(peersForksHashesInfoService.getUnknownReportedForksHashes()).thenReturn(new JSONObject());
		when(forksEpochStore.getEpochsForkHashes()).thenReturn(ImmutableMap.of());

		controller.handleHealthRequest(exchange);
		verify(sender).send("{\"unknown_reported_forks_hashes\":{},\"fork_vote_status\":\"VOTE_REQUIRED\","
			+ "\"executed_forks\":[],\"network_status\":\"BOOTING\","
			+ "\"current_fork\":{\"name\":\"fork1\"}}");

		controller.handleHealthRequest(exchange);
		verify(sender).send("{\"unknown_reported_forks_hashes\":{},\"fork_vote_status\":\"NO_ACTION_NEEDED\","
			+ "\"executed_forks\":[],\"network_status\":\"SYNCING\","
			+ "\"current_fork\":{\"name\":\"fork2\"}}");

		controller.handleHealthRequest(exchange);
		verify(sender).send("{\"unknown_reported_forks_hashes\":{},\"fork_vote_status\":\"VOTE_REQUIRED\","
			+ "\"executed_forks\":[],\"network_status\":\"UP\","
			+ "\"current_fork\":{\"name\":\"fork3\"}}");

		controller.handleHealthRequest(exchange);
		verify(sender).send("{\"unknown_reported_forks_hashes\":{},\"fork_vote_status\":\"NO_ACTION_NEEDED\","
			+ "\"executed_forks\":[],\"network_status\":\"STALLED\","
			+ "\"current_fork\":{\"name\":\"fork4\"}}");
	}
}
