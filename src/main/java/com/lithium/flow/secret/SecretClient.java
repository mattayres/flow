/*
 * Copyright 2015 Lithium Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lithium.flow.secret;

import static com.google.common.base.Preconditions.checkNotNull;

import com.lithium.flow.config.Config;
import com.lithium.flow.config.ConfigBuilder;
import com.lithium.flow.util.Logs;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.goebl.david.Response;
import com.goebl.david.Webb;

/**
 * @author Matt Ayres
 */
public class SecretClient {
	private static final Logger log = Logs.getLogger();

	private final String url;
	private final String token;

	public SecretClient(@Nonnull String url, @Nonnull String token) {
		this.url = checkNotNull(url);
		this.token = checkNotNull(token);
	}

	@Nullable
	public String read(@Nonnull String key) throws IOException {
		Response<JSONObject> response = Webb.create()
				.get(url + "/v1/secret/" + key)
				.header("X-Vault-Token", token)
				.asJsonObject();

		log.debug("read: {}", response.getStatusCode());

		if (response.getStatusCode() == 404) {
			return null;
		}

		JSONObject responseJson = response.getBody();
		try {
			JSONObject data = responseJson.getJSONObject("data");
			return data.getString("value");
		} catch (JSONException e) {
			throw new IOException(e);
		}
	}

	public void write(@Nonnull String key, @Nonnull String value) throws IOException {
		JSONObject requestJson = new JSONObject();
		try {
			requestJson.put("value", value);
		} catch (JSONException e) {
			throw new IOException(e);
		}

		Response<String> response = Webb.create()
				.post(url + "/v1/secret/" + key)
				.header("X-Vault-Token", token)
				.header("Content-Type", "application/json")
				.body(requestJson)
				.asString();

		log.debug("write: {}", response.getStatusCode());
	}

	public void delete(@Nonnull String key) throws IOException {
		Response<String> response = Webb.create()
				.delete(url + "/v1/secret/" + key)
				.header("X-Vault-Token", token)
				.header("Content-Type", "application/json")
				.asString();

		log.debug("delete: {}", response.getStatusCode());
	}

	@Nonnull
	public static Config populate(@Nonnull Config config) throws IOException {
		checkNotNull(config);

		String url = config.getString("secret.url");
		String token = config.getString("secret.token");
		String prefix = config.getString("secret.prefix", "");
		SecretClient client = new SecretClient(url, token);

		ConfigBuilder builder = config.toBuilder();
		for (String secret : config.getList("secret.list")) {
			String value = client.read(prefix + secret);
			if (value != null) {
				builder.setString(secret, value);
			}
		}
		return builder.build();
	}
}
