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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

/**
 * @author Matt Ayres
 */
public class RetryingFunction<T, R, E extends Exception> {
	private final CheckedFunction<T, R, E> function;
	private final int tries;

	public RetryingFunction(int tries, @Nonnull CheckedFunction<T, R, E> function) {
		this.function = checkNotNull(function);
		this.tries = tries;
	}

	@Nonnull
	public R get(@Nonnull T input) throws E {
		checkNotNull(input);

		if (tries == 1) {
			return function.apply(input);
		}

		List<Exception> exceptions = null;

		int i = tries;
		do {
			try {
				return function.apply(input);
			} catch (Exception e) {
				if (exceptions == null) {
					exceptions = new ArrayList<>();
				}
				exceptions.add(e);
			}
		} while (--i > 0);

		throw new RetryingException(exceptions);
	}
}
