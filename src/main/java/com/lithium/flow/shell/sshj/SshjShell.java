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

import com.lithium.flow.access.Login;
import com.lithium.flow.filer.Filer;
import com.lithium.flow.shell.Exec;
import com.lithium.flow.shell.Shell;
import com.lithium.flow.shell.Tunnel;
import com.lithium.flow.shell.Tunneling;
import com.lithium.flow.util.Logs;

import java.io.IOException;
import java.net.URI;

import javax.annotation.Nonnull;

import org.slf4j.Logger;

import net.schmizz.sshj.userauth.UserAuthException;

/**
 * @author Matt Ayres
 */
public class SshjShell implements Shell {
	private static final Logger log = Logs.getLogger();

	private final Sshj ssh;
	private final URI uri;

	public SshjShell(@Nonnull Sshj ssh, @Nonnull Login login) throws IOException {
		this.ssh = checkNotNull(ssh);
		checkNotNull(login);

		uri = URI.create("ssh://" + login.getUser() + "@" + login.getHostAndPort());

		log.debug("connect: {}", uri);
		ssh.connect(login.getHost(), login.getPortOrDefault(22));

		for (int i = 0; i <= ssh.getRetries(); i++) {
			try {
				if (login.getKeyPath() != null) {
					ssh.authPublickey(login.getUser(), ssh.loadKeys(login.getKeyPath(), login.getPass(i > 0)));
				} else {
					ssh.authPassword(login.getUser(), login.getPass(i > 0));
				}
				break;
			} catch (UserAuthException e) {
				log.warn("auth failed: {}", login);
				if (i == ssh.getRetries()) {
					throw new IOException("authentication failed: " + login
							+ " (key path: " + login.getKeyPath() + ")");
				}
			}
		}
	}

	@Override
	@Nonnull
	public URI getUri() {
		return uri;
	}

	@Override
	@Nonnull
	public synchronized Exec exec(@Nonnull String command) throws IOException {
		checkNotNull(command);
		return new SshjExec(ssh, command);
	}

	@Override
	@Nonnull
	public Tunnel tunnel(@Nonnull Tunneling tunneling) throws IOException {
		checkNotNull(tunneling);
		return new SshjTunnel(ssh, tunneling);
	}

	@Override
	@Nonnull
	public synchronized Filer getFiler() throws IOException {
		return new SshjFiler(ssh, uri);
	}

	@Override
	public void close() throws IOException {
		log.debug("close: {}", uri);
		ssh.close();
	}
}
