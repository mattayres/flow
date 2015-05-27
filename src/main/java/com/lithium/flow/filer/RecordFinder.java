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

import com.lithium.flow.util.Threader;

import java.util.Spliterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;

/**
 * @author Matt Ayres
 */
public class RecordFinder implements Spliterator<Record> {
	private final Filer filer;
	private final Threader threader;
	private final BlockingQueue<Record> queue = new LinkedBlockingQueue<>();

	private RecordFinder(@Nonnull Filer filer, @Nonnull String path, int threads) {
		this.filer = checkNotNull(filer);
		threader = new Threader(threads);
		findRecords(path);
	}

	private void findRecords(@Nonnull String path) {
		checkNotNull(path);
		threader.execute(path, () -> {
			for (Record record : filer.listRecords(path)) {
				if (record.isDir()) {
					queue.add(record);
					findRecords(record.getPath());
				} else {
					queue.add(record);
				}
			}
		});
	}

	@Override
	public boolean tryAdvance(Consumer<? super Record> action) {
		while (threader.hasWork() || queue.size() > 0) {
			try {
				Record record = queue.poll(10, TimeUnit.MILLISECONDS);
				if (record != null) {
					action.accept(record);
					return true;
				}
			} catch (InterruptedException e) {
				break;
			}
		}
		threader.shutdown();
		return false;
	}

	@Override
	public Spliterator<Record> trySplit() {
		return null;
	}

	@Override
	public long estimateSize() {
		return Long.MAX_VALUE;
	}

	@Override
	public int characteristics() {
		return IMMUTABLE;
	}

	@Nonnull
	public static Stream<Record> stream(@Nonnull Filer filer, @Nonnull String path, int threads) {
		return StreamSupport.stream(new RecordFinder(filer, path, threads), false);
	}
}
