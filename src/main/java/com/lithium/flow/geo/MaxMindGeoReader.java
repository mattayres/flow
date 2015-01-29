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
import java.util.Map;

import javax.annotation.Nonnull;

import org.slf4j.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import au.com.bytecode.opencsv.CSVReader;

/**
 * @author Matt Ayres
 */
public class MaxMindGeoReader implements GeoReader {
	private static final Logger log = Logs.getLogger();

	@Override
	@Nonnull
	public List<GeoBlock> readBlocks(@Nonnull File dir) throws IOException {
		checkNotNull(dir);

		log.debug("loading...");

		Map<String, String> countries = readCountries(new File(dir, "countries.csv"));
		log.debug("read " + countries.size() + " countries");

		Map<String, String> states = readStates(new File(dir, "states.csv"));
		log.debug("read " + states.size() + " states");

		Map<Integer, GeoDetail> locations = readLocations(new File(dir, "locations.csv"), countries);
		log.debug("read " + locations.size() + " locations");

		List<GeoBlock> blocks = readBlocks(new File(dir, "blocks.csv"), locations);
		log.debug("read " + blocks.size() + " blocks");

		log.info("ready with " + countries.size() + " countries, " + states.size() + " states, " + locations.size()
				+ " locations, " + blocks.size() + " blocks");

		return blocks;
	}

	private Map<String, String> readCountries(File file) throws IOException {
		Map<String, String> countryMap = Maps.newHashMap();
		CSVReader reader = new CSVReader(new FileReader(file));
		String[] line;
		while ((line = reader.readNext()) != null) {
			countryMap.put(line[0], line[1]);
		}
		return countryMap;
	}

	private Map<String, String> readStates(File file) throws IOException {
		Map<String, String> stateMap = Maps.newHashMap();
		CSVReader reader = new CSVReader(new FileReader(file));
		String[] line;
		while ((line = reader.readNext()) != null) {
			stateMap.put(line[1], line[2]);
		}
		return stateMap;
	}

	private Map<Integer, GeoDetail> readLocations(File file, Map<String, String> countries) throws IOException {
		Map<Integer, GeoDetail> locations = Maps.newHashMap();
		CSVReader reader = new CSVReader(new FileReader(file));
		String[] line;
		while ((line = reader.readNext()) != null) {
			int id = Integer.parseInt(line[0]);
			String countryCode = line[1].toLowerCase();
			String countryName = fix(countries.get(line[1]));
			String region = fix(line[2]);
			String city = fix(line[3]);
			String postal = fix(line[4]);
			double latitude = Double.parseDouble(line[5]);
			double longitude = Double.parseDouble(line[6]);

			GeoDetail geoDetail = new GeoDetail(city, region, postal, countryCode, countryName, latitude, longitude);
			locations.put(id, geoDetail);
		}
		return locations;
	}

	private String fix(String value) {
		if (value == null || value.trim().isEmpty()) {
			return "unknown";
		}
		if (value.startsWith("'")) {
			value = value.substring(1);
		}
		return value;
	}

	private List<GeoBlock> readBlocks(File file, Map<Integer, GeoDetail> locations) throws IOException {
		List<GeoBlock> blocks = Lists.newArrayList();
		CSVReader reader = new CSVReader(new FileReader(file));
		String[] line;
		while ((line = reader.readNext()) != null) {
			long start = Long.parseLong(line[0]);
			long end = Long.parseLong(line[1]);
			GeoDetail geoDetail = locations.get(Integer.parseInt(line[2]));
			blocks.add(new GeoBlock(start, end, geoDetail));
		}
		return blocks;
	}
}
