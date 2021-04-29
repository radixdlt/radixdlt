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

package com.radixdlt.client.application.lib.lib;

import org.json.JSONArray;
import org.json.JSONObject;

import com.radixdlt.client.lib.ClientLibErrors;
import com.radixdlt.identifiers.AccountAddress;
import com.radixdlt.identifiers.REAddr;
import com.radixdlt.utils.Pair;
import com.radixdlt.utils.UInt384;
import com.radixdlt.utils.functional.Result;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import okhttp3.ConnectionSpec;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import static com.radixdlt.client.lib.ClientLibErrors.CONNECTION_ERROR;
import static com.radixdlt.client.lib.ClientLibErrors.INVALID_NETWORK_ID;
import static com.radixdlt.client.lib.ClientLibErrors.MISSING_FIELD;
import static com.radixdlt.client.lib.ClientLibErrors.MISSING_NETWORK_ID;
import static com.radixdlt.client.lib.ClientLibErrors.NOT_A_JSON_OBJECT;
import static com.radixdlt.client.lib.ClientLibErrors.NO_CONTENT;
import static com.radixdlt.identifiers.CommonErrors.UNABLE_TO_DECODE;
import static com.radixdlt.utils.functional.Result.allOf;
import static com.radixdlt.utils.functional.Result.fromOptional;

import static java.util.Optional.ofNullable;

public class NodeClient {
	private static final MediaType MEDIA_TYPE = MediaType.parse("application/json");

	private final AtomicInteger counter = new AtomicInteger();

	private final String baseUrl;
	private final OkHttpClient client;
	private AtomicReference<Byte> magicHolder = new AtomicReference<>();

	private NodeClient(String baseUrl) {
		this.baseUrl = sanitize(baseUrl);
		this.client = new OkHttpClient.Builder()
			.connectionSpecs(List.of(ConnectionSpec.CLEARTEXT))
			.connectTimeout(30, TimeUnit.SECONDS)
			.writeTimeout(30, TimeUnit.SECONDS)
			.readTimeout(30, TimeUnit.SECONDS)
			.pingInterval(30, TimeUnit.SECONDS)
			.build();
	}

	private static String sanitize(String baseUrl) {
		if (baseUrl.endsWith("/")) {
			return baseUrl.substring(0, baseUrl.length() - 1);
		} else {
			return baseUrl;
		}
	}

	public static Result<NodeClient> connect(String baseUrl) {
		if (baseUrl == null) {
			return ClientLibErrors.BASE_URL_IS_MANDATORY.result();
		}

		return Result.ok(new NodeClient(baseUrl)).flatMap(NodeClient::tryConnect);
	}

	private static JSONObject jsonObject() {
		return new JSONObject();
	}

	private static JSONArray jsonArray() {
		return new JSONArray();
	}

	public Result<List<Pair<String, UInt384>>> callTokenBalances(REAddr addr) {
		var params = jsonArray().put(toAddress(addr));

		return call("tokenBalances", params)
			.map(this::parseTokenBalances);
	}

	public Result<JSONObject> call(String method, JSONObject params) {
		return performCall(wrap(method, params))
			.flatMap(this::parseJson);
	}

	public Result<JSONObject> call(String method, JSONArray params) {
		return performCall(wrap(method, params))
			.flatMap(this::parseJson);
	}

	public String toAddress(REAddr addr) {
		return AccountAddress.of(addr);
	}

	private Result<NodeClient> tryConnect() {
		var params = jsonArray();

		return call("networkId", params)
			.map(obj -> obj.getJSONObject("result"))
			.flatMap(this::extractNetworkIdField)
			.filter(Integer.class::isInstance, INVALID_NETWORK_ID)
			.map(Integer.class::cast)
			.onSuccess(magic -> magicHolder.set(magic.byteValue()))
			.map(__ -> this);
	}

	private Result<Object> extractNetworkIdField(JSONObject obj) {
		return fromOptional(ofNullable(obj.opt("networkId")), MISSING_NETWORK_ID);
	}

	private JSONObject wrap(String method, Object params) {
		return jsonObject()
			.put("jsonrpc", "2.0")
			.put("method", "radix." + method)
			.put("id", counter.incrementAndGet())
			.put("params", params);
	}

	private List<Pair<String, UInt384>> parseTokenBalances(JSONObject json) {
		return ofNullable(json.optJSONObject("result"))
			.flatMap(result -> ofNullable(result.optJSONArray("tokenBalances")))
			.map(this::parseTokenBalanceEntries)
			.orElseGet(List::of);
	}

	private List<Pair<String, UInt384>> parseTokenBalanceEntries(JSONArray array) {
		return parseArray(array, this::parseTokenBalanceEntry);
	}

	private <T> List<T> parseArray(JSONArray array, Function<Object, Result<T>> mapper) {
		var list = new ArrayList<T>();
		array.forEach(obj -> mapper.apply(obj).onSuccess(list::add));
		return list;
	}

	private Result<Pair<String, UInt384>> parseTokenBalanceEntry(Object obj) {
		if (!(obj instanceof JSONObject)) {
			return NOT_A_JSON_OBJECT.result();
		}

		var object = (JSONObject) obj;
		return allOf(rri(object), uint384(object, "amount"))
			.map(Pair::of);
	}

	private Result<JSONObject> parseJson(String text) {
		return Result.wrap(UNABLE_TO_DECODE, () -> new JSONObject(text));
	}

	private static Result<String> string(JSONObject object, String name) {
		return fromOptional(ofNullable(object.optString(name)), MISSING_FIELD.with(name));
	}

	private static Result<String> rri(JSONObject object) {
		return string(object, "rri");
	}

	private static Result<UInt384> uint384(JSONObject object, String name) {
		return string(object, name).flatMap(UInt384::fromString);
	}

	private Result<String> performCall(JSONObject json) {
		var body = RequestBody.create(MEDIA_TYPE, json.toString());
		var request = new Request.Builder().url(baseUrl + "/rpc").post(body).build();

		try (var response = client.newCall(request).execute(); var responseBody = response.body()) {
			return responseBody != null
				   ? Result.ok(responseBody.string())
				   : NO_CONTENT.result();
		} catch (IOException e) {
			return CONNECTION_ERROR.with(e.getMessage()).result();
		}
	}
}