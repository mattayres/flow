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

import com.lithium.flow.config.exception.IllegalConfigException;

import javax.annotation.Nonnull;

import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

/**
 * @author Matt Ayres
 */
public class ConfigObjectPool<T> extends GenericObjectPool<T> {
	public ConfigObjectPool(@Nonnull PoolableObjectFactory<T> factory, @Nonnull com.lithium.flow.config.Config config) {
		super(checkNotNull(factory), buildConfig(checkNotNull(config)));
	}

	private static Config buildConfig(@Nonnull com.lithium.flow.config.Config config) {
		Config poolConfig = new Config();
		poolConfig.lifo = config.getBoolean("pool.lifo", true);
		poolConfig.maxActive = config.getInt("pool.maxActive", Runtime.getRuntime().availableProcessors());
		poolConfig.maxIdle = config.getInt("pool.maxIdle", -1);
		poolConfig.minIdle = config.getInt("pool.minIdle", 0);
		poolConfig.testOnBorrow = config.getBoolean("pool.testOnBorrow", false);
		poolConfig.testOnReturn = config.getBoolean("pool.testOnReturn", false);
		poolConfig.timeBetweenEvictionRunsMillis = config.getTime("pool.timeBetweenEvictionRunsMillis", "-1");
		poolConfig.minEvictableIdleTimeMillis = config.getTime("pool.minEvictableIdleTimeMillis", "30m");
		poolConfig.testWhileIdle = config.getBoolean("pool.testWhileIdle", false);
		poolConfig.softMinEvictableIdleTimeMillis = config.getTime("pool.softMinEvictableIdleTimeMillis", "-1");
		poolConfig.numTestsPerEvictionRun = config.getInt("pool.numTestsPerEvictionRun", 3);

		String action = config.getString("pool.whenExhaustedAction", "block");
		switch (action) {
			case "fail":
				poolConfig.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_FAIL;
				break;
			case "block":
				poolConfig.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_BLOCK;
				break;
			case "grow":
				poolConfig.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_GROW;
				break;
			default:
				throw new IllegalConfigException("pool.whenExhaustedAction", action, "string", null);
		}

		return poolConfig;
	}
}
