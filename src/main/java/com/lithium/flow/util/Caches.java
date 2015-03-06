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

import java.util.function.UnaryOperator;

import javax.annotation.Nonnull;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * @author Matt Ayres
 */
public class Caches {
	@Nonnull
	public static <K, V> LoadingCache<K, V> build(@Nonnull CheckedFunction<K, V, Exception> function) {
		checkNotNull(function);
		return build(function, b -> b);
	}

	@Nonnull
	@SuppressWarnings("unchecked")
	public static <K, V> LoadingCache<K, V> build(@Nonnull CheckedFunction<K, V, Exception> function,
			@Nonnull UnaryOperator<CacheBuilder<K, V>> operator) {
		checkNotNull(function);
		checkNotNull(operator);
		return operator.apply((CacheBuilder<K, V>) CacheBuilder.newBuilder()).build(loader(function));
	}

	@Nonnull
	public static <K, V> CacheLoader<K, V> loader(@Nonnull CheckedFunction<K, V, Exception> function) {
		checkNotNull(function);
		return new CacheLoader<K, V>() {
			@Override
			@Nonnull
			public V load(@Nonnull K key) throws Exception {
				return function.apply(key);
			}
		};
	}
}
