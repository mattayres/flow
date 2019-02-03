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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import javax.annotation.Nonnull;

/**
 * @author Matt Ayres
 */
public class Sockets {
	public static boolean check(@Nonnull String host, int port) {
		return check(host, port, 5000);
	}

	public static boolean check(@Nonnull String host, int port, long timeout) {
		checkNotNull(host);

		Socket socket = new Socket();
		try {
			socket.connect(new InetSocketAddress(host, port), timeout > Integer.MAX_VALUE ? 0 : (int) timeout);
			return socket.isConnected();
		} catch (Exception e) {
			return false;
		}
	}

	public static void waitForConnect(@Nonnull String host, int port, long timeout) throws IOException {
		long timeoutTime = System.currentTimeMillis() + timeout;
		InetSocketAddress address = new InetSocketAddress(host, port);

		while (true) {
			try (Socket socket = new Socket()) {
				socket.connect(address, (int) timeout);
				if (socket.isConnected()) {
					return;
				}
			} catch (IOException e) {
				if (System.currentTimeMillis() > timeoutTime) {
					throw e;
				}
				Sleep.softly(1000);
			}
		}
	}
}
