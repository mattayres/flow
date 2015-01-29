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

package com.lithium.flow.filer;

import com.google.common.base.Predicate;

/**
 * @author Matt Ayres
 */
public class RecordPredicates {
	public static final Predicate<Record> ALL_RECORDS = record -> true;
	public static final Predicate<Record> DIR_RECORDS = record -> record != null && record.isDir();
	public static final Predicate<Record> FILE_RECORDS = record -> record != null && record.isFile();
}
