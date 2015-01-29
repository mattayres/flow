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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.CountDownLatch;

import javax.annotation.Nonnull;

import org.slf4j.Logger;

import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.xfer.LocalFileFilter;
import net.schmizz.sshj.xfer.LocalSourceFile;

/**
 * @author Matt Ayres
 */
public class SshjUpload implements LocalSourceFile, Runnable {
	private static final Logger log = Logs.getLogger();

	private final SFTPClient sftp;
	private final String path;
	private final PipedInputStream pipeIn;
	private final PipedOutputStream pipeOut;
	private final CountDownLatch latch = new CountDownLatch(1);

	public SshjUpload(@Nonnull SFTPClient sftp, @Nonnull String path) throws IOException {
		this.sftp = checkNotNull(sftp);
		this.path = checkNotNull(path);

		pipeIn = new PipedInputStream();
		pipeOut = new PipedOutputStream(pipeIn) {
			@Override
			public void close() throws IOException {
				try {
					super.close();
				} finally {
					Sleep.softly(latch::await);
				}
			}
		};

		new Thread(this).start();
	}

	@Nonnull
	public OutputStream getOutputStream() {
		return pipeOut;
	}

	@Override
	public void run() {
		try {
			sftp.put(this, path);
		} catch (IOException e) {
			log.warn("upload failed: {}", path, e);
		} finally {
			latch.countDown();
		}
	}

	@Override
	public boolean isFile() {
		return true;
	}

	@Override
	public boolean isDirectory() {
		return false;
	}

	@Override
	public String getName() {
		return new File(path).getName();
	}

	@Override
	public long getLength() {
		return 0;
	}

	@Override
	@Nonnull
	public InputStream getInputStream() throws IOException {
		return pipeIn;
	}

	@Override
	public int getPermissions() throws IOException {
		return 0644; // octal
	}

	@Override
	public boolean providesAtimeMtime() {
		return false;
	}

	@Override
	public long getLastAccessTime() throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public long getLastModifiedTime() throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterable<? extends LocalSourceFile> getChildren(LocalFileFilter filter) throws IOException {
		throw new UnsupportedOperationException();
	}
}
