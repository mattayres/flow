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

import com.lithium.flow.access.Login;

import java.io.File;
import java.io.IOException;

import javax.annotation.Nonnull;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;

/**
 * @author Matt Ayres
 */
public class LoginSvnProvider implements SvnProvider {
	private final SVNURL url;
	private final Login login;

	public LoginSvnProvider(@Nonnull String url, @Nonnull Login login) throws SVNException {
		this.url = SVNURL.parseURIEncoded(checkNotNull(url));
		this.login = checkNotNull(login);
	}

	@Override
	@Nonnull
	public SVNURL getLocation() {
		return url;
	}

	@Override
	@Nonnull
	public SVNRepository getRepository() throws IOException {
		ISVNAuthenticationManager authManager = login.getKeyPath() == null
				? new BasicAuthenticationManager(login.getUser(), login.getPass(false))
				: new BasicAuthenticationManager(login.getUser(), new File(login.getKeyPath()),
				login.getPass(false), login.getPortOrDefault(22));

		try {
			SVNRepository repository = SVNRepositoryFactory.create(url);
			repository.setAuthenticationManager(authManager);
			return repository;
		} catch (SVNException e) {
			throw new IOException("failed to created repository: " + url, e);
		}
	}

	@Override
	public void releaseRepository(@Nonnull SVNRepository repository) {
		repository.closeSession();
	}

	@Override
	public void close() {
	}
}
