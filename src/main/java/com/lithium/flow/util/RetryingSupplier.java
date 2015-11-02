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
public class RetryingSupplier<T, E extends Exception> {
	private final CheckedSupplier<T, E> supplier;
	private final int tries;
	private final long delay;

	public RetryingSupplier(int tries, @Nonnull CheckedSupplier<T, E> supplier) {
		this(tries, 0, supplier);
	}

	public RetryingSupplier(int tries, long delay, @Nonnull CheckedSupplier<T, E> supplier) {
		this.supplier = checkNotNull(supplier);
		this.tries = tries;
		this.delay = delay;
	}

	@Nonnull
	public T get() throws E {
		if (tries == 1) {
			return supplier.get();
		}

		List<Exception> exceptions = null;

		int i = tries;
		do {
			try {
				return supplier.get();
			} catch (Exception e) {
				if (exceptions == null) {
					exceptions = new ArrayList<>();
				}
				exceptions.add(e);

				if (!Sleep.softly(delay)) {
					break;
				}
			}
		} while (--i > 0);

		throw new RetryingException(exceptions);
	}
}
