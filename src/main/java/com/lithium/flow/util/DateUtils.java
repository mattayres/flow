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
import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME;

import com.lithium.flow.config.Config;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

/**
 * @author Matt Ayres
 */
public class DateUtils {
	public static long toMillis(@Nonnull String date) {
		return toMillis(date, System::currentTimeMillis);
	}

	public static long toMillis(@Nonnull String date, @Nonnull Supplier<Long> clock) {
		checkNotNull(date);
		checkNotNull(clock);

		long time = 0;
		BinaryOperator<Long> op = (t, d) -> t;

		for (String part : Splitter.on(' ').split(date)) {
			if (part.equals("NOW")) {
				time = clock.get();
			} else if (part.startsWith("NOW~")) {
				long round = TimeUtils.getMillisValue(part.substring(4));
				time = clock.get() / round * round;
			} else if (part.equals("+")) {
				op = (t, d) -> t + d;
			} else if (part.equals("-")) {
				op = (t, d) -> t - d;
			} else if (!part.contains("-")) {
				long diff = TimeUtils.getMillisValue(part);
				time = op.apply(time, diff);
			} else {
				time = ZonedDateTime.parse(part, ISO_ZONED_DATE_TIME).toInstant().toEpochMilli();
			}
		}

		return time;
	}

	@Nonnull
	public static List<String> getDates(long startTime, long endTime, @Nonnull DateTimeFormatter formatter) {
		checkNotNull(formatter);

		List<String> dates = Lists.newArrayList();
		ZonedDateTime dateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(startTime), UTC);
		while (dateTime.toInstant().toEpochMilli() < endTime) {
			dates.add(formatter.format(dateTime));
			dateTime = dateTime.plusDays(1);
		}
		return dates;
	}

	@Nonnull
	public static List<String> getDates(@Nonnull Config config, @Nonnull DateTimeFormatter formatter) {
		checkNotNull(config);
		checkNotNull(formatter);

		long startTime = toMillis(config.getString("start"));
		long endTime = toMillis(config.getString("end"));
		return getDates(startTime, endTime, formatter);
	}

	@Nonnull
	public static List<String> getDates(@Nonnull Config config) {
		checkNotNull(config);

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(config.getString("format"));
		return getDates(config, formatter);
	}
}
