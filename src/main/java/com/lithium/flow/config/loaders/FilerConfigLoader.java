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

package com.lithium.flow.config.loaders;

import com.lithium.flow.config.Config;
import com.lithium.flow.config.ConfigLoader;
import com.lithium.flow.filer.Filer;
import com.lithium.flow.filer.Filers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Jigar joshi
 */
public class FilerConfigLoader implements ConfigLoader {
	private Filer filer;

	public FilerConfigLoader(@Nullable Filer filer) {
		checkNotNull(filer);
		this.filer = filer;
	}

	public FilerConfigLoader(@Nullable Config config) throws IOException {
		checkNotNull(config);
		this.filer = Filers.buildFiler(config);
	}

	@Override
	@Nullable
	public InputStream getInputStream(@Nonnull String path) throws IOException {
		checkNotNull(path);
		return filer.readFile(path);
	}
}
