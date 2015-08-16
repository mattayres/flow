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

import com.lithium.flow.access.Prompt;
import com.lithium.flow.config.Config;

import java.io.File;
import java.io.IOException;

import javax.annotation.Nonnull;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.SecurityUtils;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

/**
 * @author Matt Ayres
 */
public class Sshj extends SSHClient {
	private final boolean pty;
	private final int retries;

	public Sshj(@Nonnull Config config, @Nonnull Prompt prompt) throws IOException {
		checkNotNull(config);
		checkNotNull(prompt);

		getConnection().setMaxPacketSize(config.getInt("shell.maxPacketSize", getConnection().getMaxPacketSize()));
		getConnection().setWindowSize(config.getLong("shell.windowSize", getConnection().getWindowSize()));

		long defaultTimeout = config.getTime("shell.timeout", "10s");
		getConnection().setTimeoutMs((int) config.getTime("shell.timeout.connection", String.valueOf(defaultTimeout)));
		getTransport().setTimeoutMs((int) config.getTime("shell.timeout.transport", String.valueOf(defaultTimeout)));

		if (config.getBoolean("shell.compress", true)) {
			useCompression();
		}

		if (config.getBoolean("shell.promiscuous", true)) {
			addHostKeyVerifier(new PromiscuousVerifier());
		} else {
			if (config.getBoolean("shell.loadKnownHosts", true)) {
				if (config.containsKey("shell.knownHosts")) {
					loadKnownHosts(new File(config.getString("shell.knownHosts")));
				} else {
					loadKnownHosts();
				}
			}

			addHostKeyVerifier((host, port, key) -> {
				String fingerprint = SecurityUtils.getFingerprint(key);
				String name = "fingerprint[" + fingerprint + "]@" + host;
				String message = "Enter yes/no to verify fingerprint " + fingerprint + " for " + host + ": ";
				return prompt.prompt(name, message, false, false).equals("yes");
			});
		}

		pty = config.getBoolean("shell.pty", false);
		retries = config.getInt("shell.retries", 3);
	}

	public boolean isPty() {
		return pty;
	}

	public int getRetries() {
		return retries;
	}
}
