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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Matt Ayres
 */
public class LoopQueue<T> {
	private final int capacity;
	private final BlockingQueue<T> queue;
	private final List<LoopThread> threads = new CopyOnWriteArrayList<>();

	/**
	 * @deprecated use {@link #LoopQueue()} and {@link #forList(Consumer)} instead.
	 */
	@Deprecated
	public LoopQueue(@Nonnull Consumer<List<T>> consumer) {
		this(Integer.MAX_VALUE);
		forList(consumer);
	}

	/**
	 * @deprecated use {@link #LoopQueue(int)} and {@link #forList(Consumer)} instead.
	 */
	@Deprecated
	public LoopQueue(int capacity, @Nonnull Consumer<List<T>> consumer) {
		this(capacity);
		forList(consumer);
	}

	public LoopQueue() {
		this(Integer.MAX_VALUE);
	}

	public LoopQueue(int capacity) {
		this(capacity, (Comparator<T>) null);
	}

	public LoopQueue(int capacity, @Nullable Comparator<T> comparator) {
		this.capacity = capacity;
		queue = comparator == null
				? new LinkedBlockingQueue<T>(capacity)
				: new PriorityBlockingQueue<>(capacity, comparator);
	}

	public void forList(@Nonnull Consumer<List<T>> consumer) {
		threads.add(new LoopThread(10, () -> {
			if (!queue.isEmpty()) {
				List<T> list = new ArrayList<>();
				queue.drainTo(list);
				consumer.accept(list);
			}
		}));
	}

	public void forEach(@Nonnull Consumer<T> consumer) {
		forList(list -> list.forEach(consumer::accept));
	}

	public void add(@Nonnull T element) {
		checkNotNull(element);
		Sleep.until(() -> queue.size() < capacity && queue.offer(element));
	}

	public void finish() {
		Sleep.until(queue::isEmpty);
		threads.forEach(LoopThread::finish);
	}
}
