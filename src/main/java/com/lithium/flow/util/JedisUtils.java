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

package com.lithium.flow.util;

import static com.google.common.base.Preconditions.checkNotNull;

import com.lithium.flow.config.Config;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Protocol;

/**
 * @author Matt Ayres
 */
public class JedisUtils {
	@Nonnull
	public static ThreadLocal<Jedis> buildTL(@Nonnull Config config) {
		checkNotNull(config);
		return ThreadLocal.withInitial(() -> build(config));
	}

	@Nonnull
	public static ThreadLocal<Jedis> buildTL(@Nonnull Config config, @Nonnull Consumer<Jedis> consumer) {
		checkNotNull(config);
		return ThreadLocal.withInitial(() -> {
			Jedis jedis = build(config);
			consumer.accept(jedis);
			return jedis;
		});
	}

	@Nonnull
	public static Jedis build(@Nonnull Config config) {
		checkNotNull(config);
		String host = config.getString("redis.host", "localhost");
		int port = config.getInt("redis.port", Protocol.DEFAULT_PORT);
		int timeout = (int) config.getTime("redis.timeout", String.valueOf(Protocol.DEFAULT_TIMEOUT));
		return new Jedis(host, port, timeout);
	}

	public static void parallel(@Nonnull Config config, @Nonnull String pattern,
			@Nonnull BiConsumer<Jedis, String> consumer) {
		checkNotNull(config);
		checkNotNull(pattern);
		checkNotNull(consumer);
		ThreadLocal<Jedis> jedisTL = buildTL(config);
		jedisTL.get().keys(pattern).parallelStream().forEach(key -> consumer.accept(jedisTL.get(), key));
	}
}
