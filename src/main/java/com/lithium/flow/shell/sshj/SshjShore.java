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

package com.lithium.flow.shell.sshj;

import static com.google.common.base.Preconditions.checkNotNull;

import com.lithium.flow.access.Access;
import com.lithium.flow.access.Login;
import com.lithium.flow.config.Config;
import com.lithium.flow.shell.Shell;
import com.lithium.flow.shell.Shore;
import com.lithium.flow.shell.util.DecoratedShell;
import com.lithium.flow.util.Caches;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nonnull;

import org.apache.commons.io.IOUtils;

import com.google.common.cache.LoadingCache;
import com.google.common.collect.Sets;

/**
 * @author Matt Ayres
 */
public class SshjShore implements Shore {
	private final Config config;
	private final Access access;
	private final Set<Shell> shells = Sets.newCopyOnWriteArraySet();
	private final LoadingCache<String, Lock> locks = Caches.build(key -> new ReentrantLock());

	public SshjShore(@Nonnull Config config, @Nonnull Access access) {
		this.config = checkNotNull(config);
		this.access = checkNotNull(access);
	}

	@Override
	@Nonnull
	public Shell getShell(@Nonnull String host) throws IOException {
		checkNotNull(host);
		return getShell(access.getLogin(host));
	}

	@Override
	@Nonnull
	public Shell getShell(@Nonnull Login login) throws IOException {
		checkNotNull(login);

		Sshj client = new Sshj(config, access.getPrompt());

		Lock lock = locks.getUnchecked(login.getHost());
		lock.lock();
		try {
			Shell shell = new SshjShell(client, login);
			shells.add(shell);
			return new DecoratedShell(shell) {
				@Override
				public void close() throws IOException {
					shells.remove(shell);
					super.close();
				}
			};
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void close() throws IOException {
		shells.forEach(IOUtils::closeQuietly);
	}
}
