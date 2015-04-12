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

package com.lithium.flow.compress;

import static com.google.common.base.Preconditions.checkNotNull;

import com.lithium.flow.util.CheckedSupplier;
import com.lithium.flow.util.Lazy;

import java.util.List;

import javax.annotation.Nonnull;

import com.google.common.collect.Lists;

/**
 * @author Matt Ayres
 */
public class CoderFactory {
	private final List<Spec> specs = Lists.newCopyOnWriteArrayList();

	@Nonnull
	public CoderFactory register(@Nonnull String extension, @Nonnull Class<? extends Coder> clazz) {
		checkNotNull(extension);
		checkNotNull(clazz);
		specs.add(new Spec(extension, clazz));
		return this;
	}

	@Nonnull
	public Coder getCoder(@Nonnull String path) {
		checkNotNull(path);
		return specs.stream()
				.filter(spec -> spec.matches(path))
				.findFirst()
				.orElseThrow(() -> new RuntimeException("unknown coder for path: " + path))
				.getCoder();
	}

	private static class Spec {
		private final String extension;
		private final Lazy<Coder> coder;

		private Spec(@Nonnull String extension, @Nonnull Class<? extends Coder> clazz) {
			this.extension = checkNotNull(extension);
			checkNotNull(clazz);
			coder = new Lazy<>((CheckedSupplier<Coder, Exception>) clazz::newInstance);
		}

		private boolean matches(@Nonnull String path) {
			checkNotNull(path);
			return extension.isEmpty() || path.equals(extension) || path.endsWith("." + extension);
		}

		@Nonnull
		public Coder getCoder() {
			return coder.get();
		}
	}
}
