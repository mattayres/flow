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

package com.lithium.flow.rx;

import static com.google.common.base.Preconditions.checkNotNull;

import com.lithium.flow.filer.FileDiff;
import com.lithium.flow.filer.Filer;
import com.lithium.flow.filer.Record;
import com.lithium.flow.util.Logs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import rx.Observable;
import rx.functions.Func1;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;

/**
 * @author Matt Ayres
 */
public class RxFiles {
	private static final Logger log = Logs.getLogger();

	@Nonnull
	public static Func1<String, Record> record(@Nonnull Filer filer) {
		checkNotNull(filer);
		return path -> {
			try {
				return filer.getRecord(path);
			} catch (IOException e) {
				throw new FunctionException("failed to get file record: " + path, e);
			}
		};
	}

	@Nonnull
	public static Func1<FileDiff, Boolean> changed(long maxHash) {
		return diff -> {
			try {
				Record destRecord = diff.getDestFiler().getRecord(diff.getDestPath());
				boolean sameSize = diff.getSrcSize() == destRecord.getSize();
				boolean sameTime = diff.getSrcTime() / 1000 == destRecord.getTime() / 1000;

				boolean changed;
				if (!sameSize) {
					changed = true;
				} else if (sameTime) {
					changed = false;
				} else if (maxHash > 0 && destRecord.getSize() < maxHash) {
					String srcHash = diff.getSrcFiler().getHash(diff.getSrcPath(), "md5", "base16");
					String destHash = diff.getDestFiler().getHash(diff.getDestPath(), "md5", "base16");
					changed = !srcHash.equals(destHash);

					if (!changed) {
						diff.getDestFiler().setFileTime(diff.getDestPath(), diff.getSrcTime());
					}
				} else {
					changed = true;
				}

				if (changed && log.isDebugEnabled()) {
					synchronized (log) {
						log.debug("record: {}", destRecord);
						String sizeEq = diff.getSrcSize() == destRecord.getSize() ? "==" : "!=";
						String timeEq = diff.getSrcTime() == destRecord.getTime() ? "==" : "!=";
						log.debug("  size: {} {} {}", diff.getSrcSize(), sizeEq, destRecord.getSize());
						log.debug("  time: {} {} {}", diff.getSrcTime(), timeEq, destRecord.getTime());
					}
				}
				return changed;
			} catch (IOException e) {
				throw new FunctionException("failed to get file record: " + diff.getDestPath(), e);
			}
		};
	}

	@Nonnull
	public static Func1<FileDiff, String> copy() {
		return diff -> {
			InputStream in = null;
			OutputStream out = null;
			try {
				diff.getDestFiler().createDirs(new File(diff.getDestPath()).getParent());

				in = diff.getSrcFiler().readFile(diff.getSrcPath());
				out = diff.getDestFiler().writeFile(diff.getDestPath());
				IOUtils.copy(in, out);
				out.close();
				in.close();

				diff.getDestFiler().setFileTime(diff.getDestPath(), diff.getSrcTime());

				return diff.getDestFiler().getUri() + ":" + diff.getDestPath();
			} catch (IOException e) {
				throw new FunctionException("failed to copy: " + diff.getSrcPath() + " to " + diff.getDestPath(), e);
			} finally {
				IOUtils.closeQuietly(in);
				IOUtils.closeQuietly(out);
			}
		};
	}

	@Nonnull
	public static Func1<List<Record>, Observable<Record>> sort() {
		return records -> {
			records = Lists.newArrayList(Iterables.filter(records, Predicates.notNull()));
			Collections.sort(records, (record1, record2) -> -Longs.compare(record1.getSize(), record2.getSize()));
			return Observable.from(records);
		};
	}
}
