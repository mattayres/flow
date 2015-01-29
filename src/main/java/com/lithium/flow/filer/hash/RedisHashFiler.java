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

package com.lithium.flow.filer.hash;

import static com.google.common.base.Preconditions.checkNotNull;

import com.lithium.flow.config.Config;
import com.lithium.flow.filer.DecoratedFiler;
import com.lithium.flow.filer.Filer;
import com.lithium.flow.util.JedisUtils;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.commons.io.IOUtils;

import com.google.common.collect.Lists;

import redis.clients.jedis.Jedis;

/**
 * @author Matt Ayres
 */
public class RedisHashFiler extends DecoratedFiler {
	private final ThreadLocal<Jedis> jedisTL;
	private final List<Jedis> jedisList = Lists.newCopyOnWriteArrayList();

	public RedisHashFiler(@Nonnull Filer delegate, @Nonnull Config config) {
		super(checkNotNull(delegate));
		checkNotNull(config);
		jedisTL = JedisUtils.buildTL(config.prefix("hash"), jedisList::add);
	}

	@Override
	@Nonnull
	public String getHash(@Nonnull String path, @Nonnull String hash, @Nonnull String base) throws IOException {
		String value = jedisTL.get().hget(path, hash);
		if (value == null) {
			value = super.getHash(path, hash, base);
			jedisTL.get().hset(path, hash, value);
		}
		return value;
	}

	@Override
	public void close() throws IOException {
		jedisList.forEach(IOUtils::closeQuietly);
		super.close();
	}
}
