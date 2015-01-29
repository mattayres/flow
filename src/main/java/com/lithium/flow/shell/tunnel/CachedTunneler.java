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

package com.lithium.flow.shell.tunnel;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.lithium.flow.util.Exceptions.unchecked;

import com.lithium.flow.shell.Tunnel;
import com.lithium.flow.shell.Tunneler;
import com.lithium.flow.util.Caches;
import com.lithium.flow.util.UncheckedException;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Triple;

import com.google.common.cache.LoadingCache;

/**
 * @author Matt Ayres
 */
public class CachedTunneler implements Tunneler {
	private final Tunneler delegate;
	private final LoadingCache<Triple<String, Integer, String>, Tunnel> tunnels;

	public CachedTunneler(@Nonnull Tunneler delegate) {
		this.delegate = checkNotNull(delegate);
		tunnels = Caches.build(triple -> delegate.getTunnel(triple.getLeft(), triple.getMiddle(), triple.getRight()));
	}

	@Override
	@Nonnull
	public Tunnel getTunnel(@Nonnull String host, int port, @Nullable String through) throws IOException {
		try {
			return unchecked(() -> tunnels.get(Triple.of(host, port, through)));
		} catch (UncheckedException e) {
			throw e.unwrap(IOException.class);
		}
	}

	@Override
	public void close() throws IOException {
		delegate.close();
	}
}
