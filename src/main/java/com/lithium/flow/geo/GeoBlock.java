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

package com.lithium.flow.geo;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nonnull;

/**
 * @author Matt Ayres
 */
public class GeoBlock {
	private final long startIp;
	private final long endIp;
	private final GeoDetail geoDetail;

	public GeoBlock(long start, long end, @Nonnull GeoDetail geoDetail) {
		this.startIp = start;
		this.endIp = end;
		this.geoDetail = checkNotNull(geoDetail);
	}

	public boolean inRange(long ip) {
		return ip >= startIp && ip <= endIp;
	}

	public boolean isAfter(long ip) {
		return ip < startIp;
	}

	public boolean isBefore(long ip) {
		return ip > endIp;
	}

	@Nonnull
	public GeoDetail getGeoDetail() {
		return geoDetail;
	}
}
