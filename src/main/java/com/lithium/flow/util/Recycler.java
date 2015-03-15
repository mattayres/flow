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

import java.io.Closeable;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import org.slf4j.Logger;

import com.google.common.base.Throwables;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Sets;

/**
 * @author Matt Ayres
 */
public class Recycler<K, V extends Closeable> {
	private static final Logger log = Logs.getLogger();

	private final long time;
	private final LoadingCache<K, V> cache;
	private final LoadingCache<V, Bin> bins;
	private final ScheduledExecutorService service = Executors.newScheduledThreadPool(1);

	public Recycler(@Nonnull Config config, @Nonnull Loader<K, V> loader) {
		checkNotNull(config);
		checkNotNull(loader);

		time = config.getTime("recycle.time", "1m");
		cache = Caches.build(loader::load);
		bins = Caches.build(key -> new Bin());
	}

	@Nonnull
	public Reusable<V> get(@Nonnull K key) throws IOException {
		checkNotNull(key);

		V value;
		try {
			value = cache.get(key);
		} catch (ExecutionException e) {
			Throwables.propagateIfInstanceOf(e.getCause(), IOException.class);
			throw Throwables.propagate(e.getCause());
		}

		Bin bin = bins.getUnchecked(value);

		Reusable<V> reusable = new Reusable<V>() {
			@Override
			@Nonnull
			public V get() {
				return value;
			}

			@Override
			public void close() {
				service.schedule(() -> {
					try {
						bin.close(this, () -> {
							cache.invalidate(key);
							bins.invalidate(value);
							value.close();
						});
					} catch (IOException e) {
						log.warn("failed to close: {}", value, e);
					}

				}, time, TimeUnit.MILLISECONDS);
			}
		};

		return bin.open(reusable) ? reusable : get(key);
	}

	public static interface Loader<K, V> {
		@Nonnull
		V load(@Nonnull K key) throws Exception;
	}

	private static class Bin {
		private final Set<Object> set = Sets.newHashSet();
		private boolean closed;

		private synchronized boolean open(@Nonnull Object object) {
			set.add(object);
			return !closed;
		}

		private synchronized void close(@Nonnull Object object, @Nonnull Closeable closeable) throws IOException {
			if (set.remove(object) && set.isEmpty() && !closed) {
				closed = true;
				closeable.close();
			}
		}
	}
}
