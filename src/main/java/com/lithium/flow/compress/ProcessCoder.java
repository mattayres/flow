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

package com.lithium.flow.compress;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;

import com.lithium.flow.util.Unchecked;

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.annotation.Nonnull;

import org.apache.commons.io.IOUtils;

/**
 * @author Matt Ayres
 */
public class ProcessCoder implements Coder {
	private final String extension;
	private final List<String> inCommands;
	private final List<String> outCommands;

	public ProcessCoder(@Nonnull String extension, @Nonnull List<String> inCommands,
			@Nonnull List<String> outCommands) {
		this.extension = checkNotNull(extension);
		this.inCommands = checkNotNull(inCommands);
		this.outCommands = checkNotNull(outCommands);
	}

	@Override
	@Nonnull
	public InputStream wrapIn(@Nonnull InputStream in) throws IOException {
		Process process = new ProcessBuilder(inCommands).start();
		Unchecked.runAsync(() -> {
			try (OutputStream out = process.getOutputStream()) {
				IOUtils.copy(in, out);
			}
		});

		return new FilterInputStream(process.getInputStream()) {
			@Override
			public void close() throws IOException {
				super.close();
				process.destroy();
			}
		};
	}

	@Override
	@Nonnull
	public OutputStream wrapOut(@Nonnull OutputStream out, int option) throws IOException {
		CountDownLatch latch = new CountDownLatch(1);

		List<String> optionCommands = outCommands.stream()
				.map(c -> c.replace("{option}", String.valueOf(option)))
				.collect(toList());

		Process process = new ProcessBuilder(optionCommands).start();
		Unchecked.runAsync(() -> {
			try (InputStream in = process.getInputStream()) {
				IOUtils.copy(in, out);
			}
			latch.countDown();
		});

		OutputStream pout = process.getOutputStream();
		return new FilterOutputStream(pout) {
			@Override
			public void write(@Nonnull byte[] b, int off, int len) throws IOException {
				pout.write(b, off, len);
			}

			@Override
			public void close() throws IOException {
				super.close();
				try {
					process.waitFor();
					latch.await();
				} catch (InterruptedException e) {
					//
				}
			}
		};
	}

	@Override
	@Nonnull
	public String getExtension() {
		return extension;
	}
}
