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

import com.lithium.flow.util.Logs;

import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

import javax.annotation.Nonnull;

import org.slf4j.Logger;

/**
 * @author Matt Ayres
 */
public class FastGeoProvider implements GeoProvider {
	private static final Logger log = Logs.getLogger();

	private final List<GeoBlock> blocks;

	public FastGeoProvider(@Nonnull List<GeoBlock> blocks) {
		this.blocks = checkNotNull(blocks);
	}

	@Override
	@Nonnull
	public GeoDetail getGeoDetail(@Nonnull String ip) {
		checkNotNull(ip);

		long longIp = 0;
		Scanner scanner = new Scanner(ip).useDelimiter("\\.");
		try {
			longIp += scanner.nextLong() * 256 * 256 * 256;
			longIp += scanner.nextLong() * 256 * 256;
			longIp += scanner.nextLong() * 256;
			longIp += scanner.nextLong();
		} catch (InputMismatchException e) {
			return GeoDetail.unknown();
		}

		log.trace("looking for {}", longIp);

		int min = 0;
		int max = blocks.size() - 1;
		int pos;
		int num = 1000;

		while (num-- > 0) {
			pos = min + (max - min) / 2;

			log.trace("{} {} {}", min, max, pos);

			GeoBlock block = blocks.get(pos);

			if (block.inRange(longIp)) {
				return block.getGeoDetail();
			} else if (min == max) {
				return GeoDetail.unknown();
			} else if (block.isAfter(longIp)) {
				max = pos - 1;
			} else if (block.isBefore(longIp)) {
				min = pos + 1;
			} else {
				min += 2;
			}
		}

		return GeoDetail.unknown();
	}
}
