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
import static java.nio.charset.StandardCharsets.UTF_8;
import static net.schmizz.sshj.connection.channel.direct.Session.Command;

import com.lithium.flow.io.InputStreamSpliterator;
import com.lithium.flow.shell.Exec;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.io.IOUtils;

import net.schmizz.sshj.connection.channel.direct.Session;

/**
 * @author Matt Ayres
 */
public class SshjExec implements Exec {
	private final Command command;
	private final boolean pty;

	public SshjExec(@Nonnull Sshj ssh, @Nonnull String command, @Nullable Boolean pty) throws IOException {
		checkNotNull(ssh);
		checkNotNull(command);

		Session session = ssh.startSession();
		this.pty = pty != null ? pty : ssh.isPty();
		if (this.pty) {
			session.allocateDefaultPTY();
		}

		this.command = session.exec(command);
	}

	@Override
	@Nonnull
	public Stream<String> out() {
		return StreamSupport.stream(new InputStreamSpliterator(command.getInputStream(), UTF_8), false);
	}

	@Override
	@Nonnull
	public Stream<String> err() {
		return StreamSupport.stream(new InputStreamSpliterator(command.getErrorStream(), UTF_8), false);
	}

	@Override
	@Nonnull
	public Optional<Integer> exit() throws IOException {
		out().count();
		err().count();
		Integer status = command.getExitStatus();
		close();
		return Optional.ofNullable(status);
	}

	@Override
	public void close() throws IOException {
		if (pty) {
			command.close();
		} else {
			IOUtils.closeQuietly(command.getOutputStream());
			IOUtils.closeQuietly(command.getInputStream());
			IOUtils.closeQuietly(command.getErrorStream());
		}
	}
}
