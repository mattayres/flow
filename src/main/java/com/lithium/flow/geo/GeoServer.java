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

package com.lithium.flow.geo;

import com.lithium.flow.config.Config;
import com.lithium.flow.jetty.JettyServer;
import com.lithium.flow.util.Main;

import java.io.IOException;

import javax.annotation.Nonnull;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * @author Matt Ayres
 */
@WebSocket
public class GeoServer extends JettyServer {
	private final ObjectWriter writer = new ObjectMapper().writer();
	private final GeoProvider provider;

	public GeoServer(@Nonnull Config config) throws Exception {
		provider = GeoModule.buildGeoProvider(config);
		start(config, (request, response) -> this);
	}

	@Override
	@Nonnull
	protected String encode(@Nonnull Session session, @Nonnull String ip) throws IOException {
		GeoDetail detail = provider.getGeoDetail(ip);
		return writer.writeValueAsString(detail);
	}

	public static void main(String[] args) {
		Main.run();
	}
}
