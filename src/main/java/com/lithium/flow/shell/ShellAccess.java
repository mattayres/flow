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

package com.lithium.flow.shell;

import static com.google.common.base.Preconditions.checkNotNull;

import com.lithium.flow.access.Access;
import com.lithium.flow.access.Login;
import com.lithium.flow.access.Prompt;
import com.lithium.flow.config.Config;

import java.io.IOException;

import javax.annotation.Nonnull;

/**
 * @author Matt Ayres
 */
public class ShellAccess implements Access {
	private final Config config;
	private final Prompt prompt;

	public ShellAccess(@Nonnull Config config, @Nonnull Prompt prompt) {
		this.config = checkNotNull(config);
		this.prompt = checkNotNull(prompt);
	}

	@Override
	@Nonnull
	public Prompt getPrompt() {
		return prompt;
	}

	@Override
	@Nonnull
	public Login getLogin(@Nonnull String spec) throws IOException {
		checkNotNull(spec);

		String host = spec;
		String user = System.getProperty("user.name");
		int port = -1;

		int index = host.indexOf('@');
		if (index > -1) {
			user = host.substring(0, index);
			host = host.substring(index + 1);
		}
		index = host.indexOf(':');
		if (index > -1) {
			port = Integer.parseInt(host.substring(index + 1));
			host = host.substring(0, index);
		}

		user = config.getMatch("shell.users", host).orElse(user);

		String keyPath = config.getMatch("shell.keys", host).orElse(null);

		return Login.builder().setUser(user).setHost(host).setPort(port).setKeyPath(keyPath).build();
	}
}
