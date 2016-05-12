/*
 * Copyright 2016 Lithium Technologies, Inc.
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

package com.lithium.flow.stream;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

/**
 * @author Matt Ayres
 */
public class QueueSpliterator<T> extends IndefiniteSpliterator<T> {
	private final BlockingQueue<T> queue;

	public QueueSpliterator(@Nonnull BlockingQueue<T> queue) {
		this.queue = checkNotNull(queue);
	}

	@Override
	public boolean tryAdvance(Consumer<? super T> action) {
		try {
			action.accept(queue.take());
			return true;
		} catch (InterruptedException e) {
			return false;
		}
	}
}
