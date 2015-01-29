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

package com.lithium.flow.config.repos;

import com.lithium.flow.config.Config;
import com.lithium.flow.config.Repo;
import com.lithium.flow.util.LogExecutorService;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;

import com.google.common.collect.Lists;

/**
 * @author Matt Ayres
 */
public class ParallelRepo extends DecoratedRepo {
	private final int threads;

	public ParallelRepo(@Nonnull Repo delegate, @Nonnull Config config) {
		super(delegate);
		threads = config.getInt("threads", Runtime.getRuntime().availableProcessors() / 2);
	}

	@Override
	@Nonnull
	public List<Config> getConfigs() throws IOException {
		List<Config> configs = Lists.newCopyOnWriteArrayList();
		LogExecutorService service = new LogExecutorService(threads);
		getNames().forEach(name -> service.execute(name, () -> configs.add(getConfig(name))));
		service.finish();
		return configs;
	}
}
