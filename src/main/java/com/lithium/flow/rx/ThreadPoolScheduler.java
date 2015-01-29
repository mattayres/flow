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

package com.lithium.flow.rx;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nonnull;

import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.internal.schedulers.ScheduledAction;
import rx.subscriptions.Subscriptions;

/**
 * @author Matt Ayres
 */
public class ThreadPoolScheduler extends Scheduler {
	private final ScheduledExecutorService service;
	private final int parallelism;

	public ThreadPoolScheduler(int threads) {
		this("RxThreadPool", threads);
	}

	public ThreadPoolScheduler(@Nonnull String name, int threads) {
		this(Executors.newScheduledThreadPool(threads, new ThreadFactory() {
			AtomicLong counter = new AtomicLong();

			@Override
			public Thread newThread(@Nonnull Runnable runnable) {
				Thread thread = new Thread(runnable, name + "-" + counter.incrementAndGet());
				thread.setDaemon(true);
				return thread;
			}
		}), threads);
	}

	public ThreadPoolScheduler(@Nonnull ScheduledExecutorService service, int parallelism) {
		this.service = checkNotNull(service);
		this.parallelism = parallelism;
	}

	@Override
	public int parallelism() {
		return parallelism;
	}

	public void finish() {
		shutdown();
		await();
	}

	public void shutdown() {
		service.shutdown();
	}

	public void await() {
		await(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
	}

	public void await(long timeout, TimeUnit timeUnit) {
		try {
			service.awaitTermination(timeout, timeUnit);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	@Override
	@Nonnull
	public Worker createWorker() {
		return new Worker() {
			private volatile boolean unsubscribed;

			@Override
			@Nonnull
			public Subscription schedule(Action0 action) {
				ScheduledAction scheduled = new ScheduledAction(action);
				Future<?> future = service.submit(action::call);
				scheduled.add(Subscriptions.from(future));
				return scheduled;
			}

			@Override
			@Nonnull
			public Subscription schedule(Action0 action, long delayTime, TimeUnit unit) {
				ScheduledAction scheduled = new ScheduledAction(action);
				Future<?> future = service.schedule(action::call, delayTime, unit);
				scheduled.add(Subscriptions.from(future));
				return scheduled;
			}

			@Override
			public void unsubscribe() {
				unsubscribed = true;
			}

			@Override
			public boolean isUnsubscribed() {
				return unsubscribed;
			}
		};
	}
}
