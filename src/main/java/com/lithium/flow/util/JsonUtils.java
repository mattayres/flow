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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Splitter;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

/**
 * @author Matt Ayres
 */
public class JsonUtils {
	private static final ThreadLocal<JSONParser> parserTL = ThreadLocal.withInitial(
			() -> new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE));

	@Nonnull
	public static JSONObject read(@Nonnull String line) throws ParseException {
		checkNotNull(line);
		return parserTL.get().parse(line, JSONObject.class);
	}

	public static void update(@Nonnull JSONObject json, @Nonnull String path, @Nullable String value) {
		checkNotNull(json);
		checkNotNull(path);

		JSONObject current = json;
		String prefix = "";
		int len = 1;

		for (String part : Splitter.on('.').split(path)) {
			if (part.equals("$")) {
				continue;
			}

			len += part.length() + 1;

			Object object = null;
			int index = -1;

			if (part.endsWith("]")) {
				int index1 = part.indexOf('[');
				int index2 = part.indexOf(']');
				try {
					index = Integer.parseInt(part.substring(index1 + 1, index2));
					String tryPart = part.substring(0, index1);

					Object array = current.get(tryPart);
					if (array == null) {
						tryPart = prefix + tryPart;
						array = current.get(tryPart);
					}

					if (array instanceof JSONArray) {
						object = ((JSONArray) array).get(index);
						part = tryPart;
						prefix = "";
					} else {
						index = -1;
					}

				} catch (NumberFormatException e) {
					index = -1;
				}
			}

			if (prefix.length() == 0) {
				if (object == null) {
					String tryPart = part + path.substring(len);
					if (path.endsWith("." + tryPart)) {
						object = current.get(tryPart);
						if (object != null) {
							part = tryPart;
						}
					}
				}

				if (object == null) {
					object = current.get(part);
				}
			}

			if (object instanceof JSONObject) {
				current = (JSONObject) object;
			} else if (object != null) {
				if (index > -1) {
					JSONArray array = (JSONArray) current.get(part);
					Class<?> valueType = array.get(index).getClass();
					if (value != null && (valueType == Integer.class || valueType == Long.class)) {
						array.set(index, Long.valueOf(value));
					} else {
						array.set(index, value);
					}
				} else {
					Class<?> valueType = current.get(part).getClass();
					if (value != null && (valueType == Integer.class || valueType == Long.class)) {
						current.put(part, Long.valueOf(value));
					} else if (value != null) {
						current.put(part, value);
					} else {
						current.remove(part);
					}
				}
				return;
			} else {
				prefix += part + ".";
			}
		}

		throw new RuntimeException("failed to update: " + path);
	}

	@Nonnull
	public static JSONObject walk(@Nonnull String line, @Nonnull Visitor visitor) throws ParseException {
		checkNotNull(line);
		checkNotNull(visitor);
		return walk(read(line), visitor);
	}

	@Nonnull
	public static JSONObject walk(@Nonnull JSONObject json, @Nonnull Visitor visitor) {
		checkNotNull(json);
		checkNotNull(visitor);
		return walk(json, visitor, "$");
	}

	@Nonnull
	private static JSONObject walk(@Nonnull JSONObject json, @Nonnull Visitor visitor, @Nonnull String parent) {
		json.forEach((key, value) -> walk(visitor, parent, key, value));
		return json;
	}

	private static void walk(@Nonnull Visitor visitor, @Nonnull String parent, @Nonnull String key,
			@Nullable Object object) {
		if (object != null) {
			String path = parent + "." + key;
			if (object instanceof JSONObject) {
				walk((JSONObject) object, visitor, path);
			} else if (object instanceof JSONArray) {
				int i = 0;
				for (Object arrayValue : (JSONArray) object) {
					walk(visitor, parent, key + "[" + i++ + "]", arrayValue);
				}
			} else {
				visitor.visit(path, object.toString());
			}
		}
	}

	public static interface Visitor {
		void visit(@Nonnull String path, @Nonnull String value);
	}
}
