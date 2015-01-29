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

package com.lithium.flow.ioc;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.reflect.Type;
import java.util.Set;

import javax.annotation.Nonnull;

import org.picocontainer.DefaultPicoContainer;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.PicoCompositionException;
import org.picocontainer.PicoContainer;
import org.picocontainer.adapters.AbstractAdapter;

import com.google.common.collect.Sets;

/**
 * @author Matt Ayres
 */
public class PicoLocator implements Locator {
	private final MutablePicoContainer pico = new DefaultPicoContainer();
	private final Set<Class<?>> types = Sets.newConcurrentHashSet();

	@Override
	@Nonnull
	public Locator addType(@Nonnull Class<?> type) {
		checkNotNull(type);
		pico.addComponent(type);
		types.add(type);
		return this;
	}

	@Override
	@Nonnull
	public Locator addProvider(@Nonnull Class<?> type, @Nonnull Provider<?> provider) {
		checkNotNull(type);
		checkNotNull(provider);
		if (types.stream().noneMatch(type::isAssignableFrom)) {
			pico.addAdapter(new Adapter(type, provider));
			types.add(type);
		}
		return this;
	}

	@Override
	@Nonnull
	public Locator addInstance(@Nonnull Object object) {
		checkNotNull(object);
		pico.addComponent(object);
		types.add(object.getClass());
		return this;
	}

	@Override
	@Nonnull
	public <T> T getInstance(@Nonnull Class<T> type) {
		checkNotNull(type);
		T instance = pico.getComponent(type);
		if (instance == null) {
			throw new RuntimeException("no instance for type: " + type);
		}
		return instance;
	}

	public class Adapter extends AbstractAdapter {
		private final Provider<?> provider;

		public Adapter(@Nonnull Class<?> type, @Nonnull Provider<?> provider) {
			super(type, type);
			this.provider = provider;
		}

		@Override
		@Nonnull
		public Object getComponentInstance(@Nonnull PicoContainer container, @Nonnull Type into)
				throws PicoCompositionException {
			try {
				return provider.provide(PicoLocator.this);
			} catch (Exception e) {
				throw new PicoCompositionException("failed to provide type: " + into, e);
			}
		}

		@Override
		public void verify(@Nonnull PicoContainer container) throws PicoCompositionException {
		}

		@Override
		@Nonnull
		public String getDescriptor() {
			return getComponentKey().toString();
		}
	}
}
