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
import static java.util.stream.Collectors.toSet;

import com.lithium.flow.config.Config;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

/**
 * @author Matt Ayres
 */
public class JedisUtils {
	/**
	 * @deprecated use {@link #buildPool(Config)} instead.
	 */
	@Deprecated
	@Nonnull
	public static ThreadLocal<Jedis> buildTL(@Nonnull Config config) {
		checkNotNull(config);
		return ThreadLocal.withInitial(() -> build(config));
	}

	/**
	 * @deprecated use {@link #buildPool(Config)} instead.
	 */
	@Deprecated
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
	public static JedisPool buildPool(@Nonnull Config config) {
		return buildPooler(config);
	}

	@Nonnull
	public static JedisPooler buildPooler(@Nonnull Config config) {
		checkNotNull(config);

		String host = config.getString("redis.host", "localhost");
		int port = config.getInt("redis.port", Protocol.DEFAULT_PORT);
		int timeout = (int) config.getTime("redis.timeout", String.valueOf(Protocol.DEFAULT_TIMEOUT));

		JedisPoolConfig poolConfig = new JedisPoolConfig();
		poolConfig.setMinIdle(config.getInt("redis.minIdle", poolConfig.getMinIdle()));
		poolConfig.setMaxIdle(config.getInt("redis.maxIdle", poolConfig.getMaxIdle()));
		poolConfig.setMaxTotal(config.getInt("redis.maxTotal", poolConfig.getMaxTotal()));

		return new JedisPooler(poolConfig, host, port, timeout);
	}

	@Nonnull
	public static Jedis build(@Nonnull Config config) {
		checkNotNull(config);
		String host = config.getString("redis.host", "localhost");
		int port = config.getInt("redis.port", Protocol.DEFAULT_PORT);
		int timeout = (int) config.getTime("redis.timeout", String.valueOf(Protocol.DEFAULT_TIMEOUT));
		return new Jedis(host, port, timeout);
	}

	@Nonnull
	public static JedisCluster buildCluster(@Nonnull Config config) {
		checkNotNull(config);

		List<String> hosts = config.getList("redis.hosts", Collections.singletonList("localhost"));
		Set<HostAndPort> nodes = hosts.stream().map(JedisUtils::buildHostAndPort).collect(toSet());
		int timeout = (int) config.getTime("redis.timeout", String.valueOf(Protocol.DEFAULT_TIMEOUT));
		GenericObjectPoolConfig poolConfig = ConfigObjectPool2.buildConfig(config.prefix("redis"));

		return new JedisCluster(nodes, timeout, poolConfig);
	}

	@Nonnull
	public static HostAndPort buildHostAndPort(@Nonnull String host) {
		checkNotNull(host);
		int index = host.indexOf(":");
		return index == -1 ? new HostAndPort(host, Protocol.DEFAULT_PORT) :
				new HostAndPort(host.substring(0, index), Integer.parseInt(host.substring(index + 1)));
	}

	/**
	 * @deprecated use {@link JedisPooler} from {@link #buildPooler(Config)} instead.
	 */
	@Deprecated
	public static void parallel(@Nonnull Config config, @Nonnull String pattern,
			@Nonnull BiConsumer<Jedis, String> consumer) {
		checkNotNull(config);
		checkNotNull(pattern);
		checkNotNull(consumer);
		ThreadLocal<Jedis> jedisTL = buildTL(config);
		jedisTL.get().keys(pattern).parallelStream().forEach(key -> consumer.accept(jedisTL.get(), key));
	}
}
