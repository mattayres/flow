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

package com.lithium.flow.config;

import com.lithium.flow.config.loaders.FileConfigLoader;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Matt Ayres
 */
public class Configs {
	/**
	 * @return a new, empty config instance.
	 */
	@Nonnull
	public static Config empty() {
		return newBuilder().build();
	}

	/**
	 * @return a new {@link ConfigBuilder} instance.
	 */
	@Nonnull
	public static ConfigBuilder newBuilder() {
		return new BaseConfigBuilder();
	}

	/**
	 * @return a new {@link ConfigBuilder} instance.
	 */
	@Nonnull
	public static ConfigBuilder newBuilder(@Nullable Config defaults) {
		return new BaseConfigBuilder(defaults);
	}

	/**
	 * Constructs a {@link Config} based on path of 'local.config' system property. If the system property is not set
	 * then an attempt to load a 'local.config' file in the current directory is made. The config file will be able to
	 * make includes relative to its parent path.
	 *
	 * @return {@link Config}
	 * @throws java.io.IOException if the 'local.config' path doesn't exist or the system property is not set
	 */
	@Nonnull
	public static Config local() throws IOException {
		String path = System.getProperty("local.config");
		if (path == null) {
			if (new File("local.config").exists()) {
				path = "local.config";
			} else {
				throw new IOException("system property not set: local.config");
			}
		}

		File file = new File(path);
		if (!file.exists()) {
			throw new IOException("local.config file does not exist: " + path);
		}

		// this loader allows for includes relative to the local.config parent path
		ConfigLoader loader = new FileConfigLoader(file.getParent());

		return newBuilder().addLoader(loader).include(path).build();
	}

	/**
	 * @return empty {@link List} typed for {@link String}, useful for {@link Config#getList(String, List)}.
	 */
	@Nonnull
	public static List<String> emptyList() {
		return Collections.emptyList();
	}
}
