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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.text.WordUtils;
import org.slf4j.Logger;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.google.common.collect.Lists;

import au.com.bytecode.opencsv.CSVReader;

/**
 * @author Matt Ayres
 */
public class Ip2LocationGeoReader implements GeoReader {
	private static final Logger log = Logs.getLogger();

	@Override
	@Nonnull
	public List<GeoBlock> readBlocks(@Nonnull File file) throws IOException {
		checkNotNull(file);

		long time = System.currentTimeMillis();
		List<GeoBlock> blocks = Lists.newArrayList();
		Interner<String> interner = Interners.newStrongInterner();
		CSVReader reader = new CSVReader(new FileReader(file));

		String[] line;
		while ((line = reader.readNext()) != null) {
			long start = Long.parseLong(line[0]);
			long end = Long.parseLong(line[1]);
			String countryCode = fixUnknown(interner.intern(line[2].toLowerCase()));
			String countryName = fixUnknown(interner.intern(WordUtils.capitalizeFully(line[3])));
			String region = interner.intern(WordUtils.capitalizeFully(line[4]));
			String city = interner.intern(WordUtils.capitalizeFully(line[5]));
			double latitude = Double.parseDouble(line[6]);
			double longitude = Double.parseDouble(line[7]);
			String postal = line.length <= 8 ? "unknown" : fixUnknown(interner.intern(line[8]));
			String timeZone = line.length <= 9 ? "unknown" : fixUnknown(interner.intern(line[9]));

			GeoDetail detail = new GeoDetail(city, region, postal, countryCode, countryName,
					latitude, longitude, timeZone);
			GeoBlock block = new GeoBlock(start, end, detail);
			blocks.add(block);
		}
		reader.close();

		time = System.currentTimeMillis() - time;
		log.info("read {} blocks in {}ms", blocks.size(), time);
		return blocks;
	}

	private String fixUnknown(String value) {
		return value.equals("-") ? "unknown" : value;
	}
}
