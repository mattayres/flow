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

import javax.annotation.Nonnull;

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

/**
 * @author Matt Ayres
 */
public class ConfigObjectPool2<T> extends GenericObjectPool<T> {
	public ConfigObjectPool2(@Nonnull PooledObjectFactory<T> factory, @Nonnull Config config) {
		super(checkNotNull(factory), buildConfig(checkNotNull(config)));
	}

	@Nonnull
	private static GenericObjectPoolConfig buildConfig(@Nonnull Config config) {
		GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
		poolConfig.setLifo(config.getBoolean("pool.lifo", true));
		poolConfig.setMaxTotal(config.getInt("pool.maxTotal",
				config.getInt("pool.maxActive", Runtime.getRuntime().availableProcessors())));
		poolConfig.setMaxIdle(config.getInt("pool.maxIdle", -1));
		poolConfig.setMinIdle(config.getInt("pool.minIdle", 0));
		poolConfig.setTestOnCreate(config.getBoolean("pool.testOnCreate", true));
		poolConfig.setTestOnBorrow(config.getBoolean("pool.testOnBorrow", true));
		poolConfig.setTestOnReturn(config.getBoolean("pool.testOnReturn", false));
		poolConfig.setTimeBetweenEvictionRunsMillis(config.getTime("pool.timeBetweenEvictionRunsMillis", "-1"));
		poolConfig.setMinEvictableIdleTimeMillis(config.getTime("pool.minEvictableIdleTimeMillis", "30m"));
		poolConfig.setTestWhileIdle(config.getBoolean("pool.testWhileIdle", false));
		poolConfig.setSoftMinEvictableIdleTimeMillis(config.getTime("pool.softMinEvictableIdleTimeMillis", "-1"));
		poolConfig.setNumTestsPerEvictionRun(config.getInt("pool.numTestsPerEvictionRun", 3));
		poolConfig.setBlockWhenExhausted(config.getBoolean("pool.blockWhenExhausted", true));
		poolConfig.setMaxWaitMillis(config.getTime("pool.maxWaitMillis", "-1"));
		return poolConfig;
	}
}
