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

package com.lithium.flow.svn;

import static com.google.common.base.Preconditions.checkNotNull;

import com.lithium.flow.config.Config;
import com.lithium.flow.util.ConfigObjectPool;

import javax.annotation.Nonnull;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.io.SVNRepository;

/**
 * @author Matt Ayres
 */
public class PoolSvnProvider extends BasePoolableObjectFactory<SVNRepository> implements SvnProvider {
	private final SvnProvider delegate;
	private final ObjectPool<SVNRepository> pool;

	public PoolSvnProvider(@Nonnull SvnProvider delegate, @Nonnull Config config) {
		this.delegate = checkNotNull(delegate);
		pool = new ConfigObjectPool<>(this, checkNotNull(config).prefix("svn"));
	}

	@Override
	@Nonnull
	public SVNURL getLocation() {
		return delegate.getLocation();
	}

	@Override
	@Nonnull
	public SVNRepository getRepository() {
		try {
			return pool.borrowObject();
		} catch (Exception e) {
			throw new RuntimeException("failed to borrow repository", e);
		}
	}

	@Override
	public void releaseRepository(@Nonnull SVNRepository repository) {
		try {
			pool.returnObject(repository);
		} catch (Exception e) {
			throw new RuntimeException("failed to return repository", e);
		}
	}

	@Override
	public SVNRepository makeObject() throws Exception {
		return delegate.getRepository();
	}

	@Override
	public void destroyObject(@Nonnull SVNRepository repository) throws Exception {
		delegate.releaseRepository(repository);
	}

	@Override
	public void close() {
		try {
			pool.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
