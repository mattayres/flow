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

package com.lithium.flow.filer;

import static com.google.common.base.Preconditions.checkNotNull;

import com.lithium.flow.io.DataIo;
import com.lithium.flow.util.Logs;
import com.lithium.flow.util.LoopThread;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.slf4j.Logger;

import com.google.common.collect.Lists;

/**
 * @author Matt Ayres
 */
public class StatsFiler implements Filer {
	private static final Logger log = Logs.getLogger();

	private final Filer delegate;
	private final List<Stat> stats = Lists.newArrayList();
	private final Stat getUriStat = new Stat("getUri");
	private final Stat listRecordsStat = new Stat("listRecords");
	private final Stat getRecordStat = new Stat("getRecord");
	private final Stat findRecordsStat = new Stat("findRecords");
	private final Stat readFileStat = new Stat("readFile");
	private final Stat writeFileStat = new Stat("writeFile");
	private final Stat openFileStat = new Stat("openFile");
	private final Stat setFileTimeStat = new Stat("setFileTime");
	private final Stat removeFileStat = new Stat("deleteFile");
	private final Stat createDirsStat = new Stat("createDirs");
	private final Stat renameFileStat = new Stat("renameFile");
	private final Stat closeStat = new Stat("close");
	private final long startTime = System.currentTimeMillis();

	public StatsFiler(@Nonnull Filer delegate) {
		this(delegate, 0);
	}

	public StatsFiler(@Nonnull Filer delegate, int dumpInterval) {
		this.delegate = checkNotNull(delegate);
		if (dumpInterval > 0) {
			new LoopThread(dumpInterval, this::dumpStats);
		}
	}

	@Override
	@Nonnull
	public java.net.URI getUri() throws IOException {
		Token token = getUriStat.start();
		try {
			return delegate.getUri();
		} finally {
			token.finish();
		}
	}

	@Override
	@Nonnull
	public List<Record> listRecords(@Nonnull String path) throws IOException {
		Token token = listRecordsStat.start();
		try {
			return delegate.listRecords(path);
		} finally {
			token.finish();
		}
	}

	@Override
	@Nonnull
	public Record getRecord(@Nonnull String path) throws IOException {
		Token token = getRecordStat.start();
		try {
			return delegate.getRecord(path);
		} finally {
			token.finish();
		}
	}

	@Override
	@Nonnull
	public Stream<Record> findRecords(@Nonnull String path, int threads) throws IOException {
		Token token = findRecordsStat.start();
		try {
			return delegate.findRecords(path, threads);
		} finally {
			token.finish();
		}
	}

	@Override
	@Nonnull
	public InputStream readFile(@Nonnull String path) throws IOException {
		Token token = readFileStat.start();
		try {
			return delegate.readFile(path);
		} finally {
			token.finish();
		}
	}

	@Override
	@Nonnull
	public OutputStream writeFile(@Nonnull String path) throws IOException {
		Token token = writeFileStat.start();
		try {
			return delegate.writeFile(path);
		} finally {
			token.finish();
		}
	}

	@Override
	@Nonnull
	public DataIo openFile(@Nonnull String path, boolean write) throws IOException {
		Token token = openFileStat.start();
		try {
			return delegate.openFile(path, write);
		} finally {
			token.finish();
		}
	}

	@Override
	public void setFileTime(@Nonnull String path, long time) throws IOException {
		Token token = setFileTimeStat.start();
		try {
			delegate.setFileTime(path, time);
		} finally {
			token.finish();
		}
	}

	@Override
	public void deleteFile(@Nonnull String path) throws IOException {
		Token token = removeFileStat.start();
		try {
			delegate.deleteFile(path);
		} finally {
			token.finish();
		}
	}

	@Override
	public void createDirs(@Nonnull String path) throws IOException {
		Token token = createDirsStat.start();
		try {
			delegate.createDirs(path);
		} finally {
			token.finish();
		}
	}

	@Override
	public void renameFile(@Nonnull String oldPath, @Nonnull String newPath) throws IOException {
		Token token = renameFileStat.start();
		try {
			delegate.renameFile(oldPath, newPath);
		} finally {
			token.finish();
		}
	}

	@Override
	public void close() throws IOException {
		Token token = closeStat.start();
		try {
			delegate.close();
		} finally {
			token.finish();
		}

		dumpStats();
	}

	public void dumpStats() {
		long time = System.currentTimeMillis() - startTime;
		for (Stat stat : stats) {
			long avg = stat.getCount() > 0 ? stat.getTime() / stat.getCount() : 0;
			long perSec = stat.getTime() > 0 ? stat.getCount() * 1000 / stat.getTime() : 0;
			log.info("{}: {} times in {}ms ({}ms avg, {}/sec)",
					stat.getName(), stat.getCount(), stat.getTime(), avg, perSec);
		}
		log.info("time: {}ms", time);
	}

	private class Stat {
		private final String name;
		private final AtomicInteger count = new AtomicInteger();
		private final AtomicLong nanos = new AtomicLong();

		public Stat(@Nonnull String name) {
			checkNotNull(name);
			this.name = name;
			stats.add(this);
		}

		@Nonnull
		public Token start() {
			long nanoTime = System.nanoTime();
			return () -> {
				count.incrementAndGet();
				nanos.addAndGet(System.nanoTime() - nanoTime);
			};
		}

		@Nonnull
		public String getName() {
			return name;
		}

		public int getCount() {
			return count.get();
		}

		public long getTime() {
			return nanos.get() / 1000000L;
		}
	}

	private interface Token {
		void finish();
	}
}
