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

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import com.google.common.collect.Lists;

/**
 * @author Matt Ayres
 */
public class LoopQueue<T> {
	private final BlockingQueue<T> queue;

	public LoopQueue(@Nonnull Consumer<List<T>> consumer) {
		this(Integer.MAX_VALUE, consumer);
	}

	public LoopQueue(int capacity, @Nonnull Consumer<List<T>> consumer) {
		checkNotNull(consumer);

		queue = new LinkedBlockingQueue<>(capacity);

		new LoopThread(10, () -> {
			if (!queue.isEmpty()) {
				List<T> list = Lists.newArrayList();
				queue.drainTo(list);
				consumer.accept(list);
			}
		});
	}

	public void add(@Nonnull T element) {
		checkNotNull(element);
		Sleep.until(() -> queue.offer(element));
	}

	public void finish() {
		Sleep.until(queue::isEmpty);
	}
}
