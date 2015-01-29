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

package com.lithium.flow.io;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Spliterator;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.io.LineIterator;

/**
 * @author Matt Ayres
 */
public class InputStreamSpliterator implements Spliterator<String> {
	private final LineIterator it;

	public InputStreamSpliterator(@Nonnull InputStream in, @Nonnull Charset encoding) {
		checkNotNull(in);
		checkNotNull(encoding);
		it = new LineIterator(new InputStreamReader(in, encoding));
	}

	@Override
	public boolean tryAdvance(@Nonnull Consumer<? super String> action) {
		if (it.hasNext()) {
			action.accept(it.nextLine());
			return true;
		} else {
			it.close();
			return false;
		}
	}

	@Override
	@Nullable
	public Spliterator<String> trySplit() {
		return null;
	}

	@Override
	public long estimateSize() {
		return Long.MAX_VALUE;
	}

	@Override
	public int characteristics() {
		return IMMUTABLE;
	}
}
