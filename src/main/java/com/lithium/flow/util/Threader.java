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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;

import org.slf4j.Logger;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * @author Matt Ayres
 */
public class Threader {
	private static final Logger log = Logs.getLogger();

	private final ListeningExecutorService service;
	private final AtomicInteger remaining = new AtomicInteger();

	public Threader() {
		this(-1);
	}

	public Threader(int threads) {
		this(threads == -1 ? Executors.newCachedThreadPool() : Executors.newFixedThreadPool(threads));
	}

	public Threader(@Nonnull ExecutorService service) {
		this.service = MoreExecutors.listeningDecorator(checkNotNull(service));
	}

	public void execute(@Nonnull String name, @Nonnull Executable executable) {
		submit(name, executable);
	}

	@Nonnull
	public <T> ListenableFuture<T> submit(@Nonnull String name, @Nonnull Callable<T> callable) {
		checkNotNull(name);
		checkNotNull(callable);

		remaining.incrementAndGet();
		ListenableFuture<T> future = service.submit(callable);

		Futures.addCallback(future, new FutureCallback<T>() {
			@Override
			public void onSuccess(T object) {
				remaining.decrementAndGet();
				log.debug("execution finished: {}", name);
			}

			@Override
			public void onFailure(@Nonnull Throwable throwable) {
				remaining.decrementAndGet();
				log.warn("execution failed: {}", name, throwable);
			}
		});

		return future;
	}

	public void finish() {
		while (remaining.get() > 0) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		service.shutdown();
	}

	public int getRemaining() {
		return remaining.get();
	}

	@Nonnull
	public <T> Needle<T> needle() {
		return new Needle<>(this);
	}
}
