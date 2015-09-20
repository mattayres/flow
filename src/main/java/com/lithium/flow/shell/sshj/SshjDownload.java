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

import com.lithium.flow.util.Logs;
import com.lithium.flow.util.Sleep;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.CountDownLatch;

import javax.annotation.Nonnull;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.xfer.LocalDestFile;

/**
 * @author Matt Ayres
 */
public class SshjDownload implements LocalDestFile, Runnable {
	private static final Logger log = Logs.getLogger();

	private final SFTPClient sftp;
	private final String path;
	private final PipedInputStream pipeIn;
	private final PipedOutputStream pipeOut;
	private final CountDownLatch latch = new CountDownLatch(1);

	public SshjDownload(@Nonnull SFTPClient sftp, @Nonnull String path) throws IOException {
		this.sftp = checkNotNull(sftp);
		this.path = checkNotNull(path);

		pipeIn = new PipedInputStream() {
			@Override
			public void close() throws IOException {
				try {
					super.close();
				} finally {
					Sleep.softly(latch::await);
				}
			}
		};
		pipeOut = new PipedOutputStream(pipeIn);

		new Thread(this).start();
	}

	@Nonnull
	public PipedInputStream getInputStream() {
		return pipeIn;
	}

	@Override
	public void run() {
		try {
			sftp.get(path, this);
		} catch (IOException e) {
			log.warn("download failed: {}", path, e);
		} finally {
			latch.countDown();
			IOUtils.closeQuietly(pipeOut);
		}
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return pipeOut;
	}

	@Override
	public LocalDestFile getChild(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public LocalDestFile getTargetFile(String filename) throws IOException {
		return this;
	}

	@Override
	public LocalDestFile getTargetDirectory(String dirname) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setPermissions(int perms) throws IOException {
	}

	@Override
	public void setLastAccessedTime(long t) throws IOException {
	}

	@Override
	public void setLastModifiedTime(long t) throws IOException {
	}
}
