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

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.javadocmd.simplelatlng.Geohasher;
import com.javadocmd.simplelatlng.LatLng;

/**
 * @author Matt Ayres
 */
public class GeoDetail {
	private static final GeoDetail UNKNOWN = new GeoDetail("unknown", "unknown", "unknown", "unknown", "unknown", 0, 0);

	private final String city;
	private final String region;
	private final String postal;
	private final String countryCode;
	private final String countryName;
	private final double latitude;
	private final double longitude;

	public GeoDetail(@Nonnull String city, @Nonnull String region, @Nonnull String postal, @Nonnull String countryCode,
			@Nonnull String countryName, double latitude, double longitude) {
		this.city = checkNotNull(city);
		this.region = checkNotNull(region);
		this.postal = checkNotNull(postal);
		this.countryCode = checkNotNull(countryCode);
		this.countryName = checkNotNull(countryName);
		this.latitude = latitude;
		this.longitude = longitude;
	}

	@Nonnull
	public String getCity() {
		return city;
	}

	@Nonnull
	public String getRegion() {
		return region;
	}

	@Nonnull
	public String getState() {
		return region;
	}

	@Nonnull
	public String getPostal() {
		return postal;
	}

	@Nonnull
	public String getCountryCode() {
		return countryCode;
	}

	@Nonnull
	public String getCountryName() {
		return countryName;
	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	@Nonnull
	public String getGeohash() {
		return Geohasher.hash(new LatLng(latitude, longitude));
	}

	@Nonnull
	public static GeoDetail unknown() {
		return UNKNOWN;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}
}
