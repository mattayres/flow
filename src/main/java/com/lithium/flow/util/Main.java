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

import com.lithium.flow.config.Config;
import com.lithium.flow.config.Configs;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

import org.slf4j.Logger;

/**
 * @author Matt Ayres
 */
public class Main {
	private static final Logger log = Logs.getLogger();

	public static void run() {
		run(Thread.currentThread().getStackTrace()[2].getClassName());
	}

	public static void run(@Nonnull String className) {
		try {
			run(Class.forName(className));
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	@Nonnull
	public static <T> T run(@Nonnull Class<T> clazz) {
		checkNotNull(clazz);
		AtomicReference<T> ref = new AtomicReference<>();
		run(config -> {
			try {
				ref.set(clazz.getDeclaredConstructor(Config.class).newInstance(config));
			} catch (NoSuchMethodException e) {
				ref.set(clazz.getDeclaredConstructor().newInstance());
			}
		});
		return ref.get();
	}

	public static void run(@Nonnull Callback callback) {
		checkNotNull(callback);
		try {
			Config config = System.getProperty("local.config") != null ? Configs.local() : Configs.empty();

			Map<String, String> map = new HashMap<>();
			for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
				map.put(entry.getKey().toString(), entry.getValue().toString());
			}
			config = config.toBuilder().setAll(map).build();

			Logs.configure(config);
			Logs.redirect(config);
			callback.call(config);
		} catch (Exception e) {
			log.error("failed to start", e);
			System.exit(1);
		}
	}

	public static interface Callback {
		void call(@Nonnull Config config) throws Exception;
	}
}
