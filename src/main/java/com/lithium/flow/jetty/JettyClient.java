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

package com.lithium.flow.jetty;

import static com.google.common.base.Preconditions.checkNotNull;

import com.lithium.flow.config.Config;
import com.lithium.flow.util.ConfigObjectPool;
import com.lithium.flow.util.HostUtils;
import com.lithium.flow.util.Logs;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;

import com.google.common.base.Splitter;
import com.google.common.collect.Maps;

/**
 * @author Matt Ayres
 */
@WebSocket
public class JettyClient extends BasePoolableObjectFactory<Session> implements Closeable {
	private static final Logger log = Logs.getLogger();

	private final Map<Session, Token<?>> tokens = Maps.newConcurrentMap();
	private final ExecutorService service = Executors.newCachedThreadPool();
	private final AtomicInteger pos = new AtomicInteger();
	private final Config config;
	private final WebSocketClient client;
	private final ObjectPool<Session> pool;

	public JettyClient(@Nonnull Config config) throws Exception {
		this.config = checkNotNull(config);

		client = new WebSocketClient(service);
		client.getPolicy().setMaxTextMessageSize(config.getInt("maxTextMessageSize", 1024 * 1024));
		client.getPolicy().setIdleTimeout(config.getTime("idleTimeout", "1h"));
		client.start();

		pool = new ConfigObjectPool<>(this, config);
	}

	@Override
	@Nonnull
	public Session makeObject() throws IOException {
		List<String> urls = HostUtils.expand(config.getList("url", Splitter.on(' ')));
		Collections.rotate(urls, pos.decrementAndGet() % urls.size());
		for (String url : urls) {
			log.debug("connecting: {}", url);
			try {
				return client.connect(this, new URI(url), new ClientUpgradeRequest()).get();
			} catch (Exception e) {
				log.warn("failed to connect to url: {}", url, e);
			}
		}
		throw new IOException("unable to connect to any url: " + urls);
	}

	@Override
	public void destroyObject(@Nonnull Session session) throws Exception {
		session.close();
	}

	@Override
	public boolean validateObject(@Nonnull Session session) {
		return session.isOpen();
	}

	@Nonnull
	public <T> T call(@Nonnull String text, @Nonnull Decoder<T> decoder) throws IOException {
		checkNotNull(text);
		checkNotNull(decoder);

		Session session;
		try {
			session = pool.borrowObject();
		} catch (Exception e) {
			throw new IOException("failed to get session from pool", e);
		}

		Token<T> token = new Token<>(decoder);
		tokens.put(session, token);
		session.getRemote().sendStringByFuture(text);
		return token.get(config.getTime("timeout", "15s"));
	}

	@OnWebSocketMessage
	public void onInput(Session session, String input) {
		log.debug("input: {}", input);

		Token<?> token = tokens.remove(session);

		try {
			pool.returnObject(session);
		} catch (Exception e) {
			log.warn("failed to return session to pool", e);
		}

		if (token == null) {
			log.warn("no token for input: {}", input);
			return;
		}

		if (input.startsWith("ERROR: ")) {
			token.exception(new RuntimeException(input));
		} else {
			try {
				token.set(input);
			} catch (Exception e) {
				token.exception(e);
			}
		}
	}

	@Override
	public void close() {
		service.shutdown();
		try {
			client.stop();
		} catch (Exception e) {
			throw new RuntimeException("failed to close web socket client", e);
		}
	}
}
