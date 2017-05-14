/*
 * Copyright 2017 Lithium Technologies, Inc.
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

package com.lithium.flow.runner;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toMap;

import com.lithium.flow.config.Config;
import com.lithium.flow.filer.Filer;
import com.lithium.flow.filer.Record;
import com.lithium.flow.filer.RecordPath;
import com.lithium.flow.shell.Shell;
import com.lithium.flow.util.Logs;
import com.lithium.flow.util.Needle;
import com.lithium.flow.util.Once;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

/**
 * @author Matt Ayres
 */
public class RunnerSync {
	private static final Logger log = Logs.getLogger();

	private final RunnerContext context;
	private final Filer destFiler;
	private final String destDir;
	private final String libDir;
	private final List<String> paths;

	private final Filer srcFiler;
	private final Once<String> once;
	private final Shell shell;

	public RunnerSync(@Nonnull Config config, @Nonnull RunnerContext context, @Nonnull Filer destFiler) throws IOException {
		checkNotNull(config);
		this.context = checkNotNull(context);
		this.destFiler = checkNotNull(destFiler);

		once = new Once<>(destFiler::createDirs);

		String host = config.getString("host");
		destDir = config.getString("dest.dir");
		libDir = config.getString("lib.dir");
		paths = config.getList("paths", Collections.emptyList());

		srcFiler = context.getFiler();

		shell = context.getShore().getShell(host);

		log.debug("src.dir: {}", context.getSrcDir());
		log.debug("dest.dir: {}", destDir);
	}

	public void sync() throws IOException {
		Needle needle = context.getSyncThreader().needle();

		List<Record> srcRecords = context.getRecords(paths);
		context.getHostsMeasure().incTodo();
		context.getFilesMeasure().addTodo(srcRecords.size());
		context.getJarsMeasure().addTodo(context.getJars().size());

		needle.execute("jars", () -> {
			destFiler.createDirs(libDir);

			Map<String, Record> jarRecords = destFiler.findRecords(destDir, 1)
					.filter(Record::isFile).collect(toMap(Record::getPath, r -> r));

			for (String jar : context.getJars()) {
				Record srcRecord = srcFiler.getRecord(jar);
				String name = srcRecord.getName();
				Record destRecord = jarRecords.get(name);

				boolean sameSize = destRecord != null && srcRecord.getSize() == destRecord.getSize();
				if (!sameSize) {
					needle.execute("jar:" + jar, () -> copyJar(srcRecord, libDir + "/" + name));
				} else {
					context.getJarsMeasure().incDone();
				}
			}
		});

		needle.execute("paths", () -> {
			Map<String, Record> destRecords = destFiler.findRecords(destDir, 1)
					.filter(Record::isFile).collect(toMap(Record::getPath, r -> r));

			for (Record srcRecord : srcRecords) {
				String srcPath = context.normalize(srcRecord.getPath());
				String destPath = srcPath.replace(context.getNormalizedSrcDir(), destDir);
				Record destRecord = destRecords.remove(destPath);

				boolean sameSize = destRecord != null && srcRecord.getSize() == destRecord.getSize();
				boolean sameTime = destRecord != null && srcRecord.getTime() / 1000 == destRecord.getTime() / 1000;

				if (!sameSize || !sameTime) {
					context.getCopiedMeasure().addTodo(srcRecord.getSize());
					needle.execute("path:" + srcRecord.getPath(), () -> copyFile(srcRecord));
				} else {
					context.getFilesMeasure().incDone();
				}
			}

			List<String> destDirs = new ArrayList<>();
			paths.forEach(path -> destDirs.add(path.replace(context.getSrcDir(), destDir)));
			context.getClasspath(destDir).stream()
					.filter(path -> !RecordPath.getName(path).contains("."))
					.forEach(destDirs::add);

			for (Record destRecord : destRecords.values()) {
				for (String dir : destDirs) {
					if (destRecord.getPath().startsWith(dir)) {
						log.debug("delete: {}", destRecord.getPath());
						destFiler.deleteFile(destRecord.getPath());
						context.getDeletedMeasure().incDone();
						break;
					}
				}
			}
		});

		needle.finish();
		context.getHostsMeasure().incDone();
	}

	private void copyJar(@Nonnull Record srcRecord, @Nonnull String destPath) throws IOException {
		String srcPath = srcRecord.getPath();
		if (!context.getJarProvider().copy(srcPath, shell, destFiler, libDir)) {
			sync(srcPath, destPath, srcRecord.getTime());
		}

		context.getJarsMeasure().incDone();
	}

	private void copyFile(@Nonnull Record srcRecord) throws IOException {
		String srcPath = context.normalize(srcRecord.getPath());
		String destPath = srcPath.replace(context.getNormalizedSrcDir(), destDir);

		once.accept(RecordPath.getFolder(destPath));

		sync(srcPath, destPath, srcRecord.getTime());

		context.getCopiedMeasure().addDone(srcRecord.getSize());
		context.getFilesMeasure().incDone();
	}

	private void sync(@Nonnull String srcPath, @Nonnull String destPath, long time) throws IOException {
		log.debug("sync: {}", destPath);

		try (InputStream in = srcFiler.readFile(srcPath)) {
			try (OutputStream out = destFiler.writeFile(destPath)) {
				IOUtils.copy(in, out);
			}
		}

		destFiler.setFileTime(destPath, time);
	}
}
