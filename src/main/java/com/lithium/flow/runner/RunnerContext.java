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
import static java.util.stream.Collectors.toList;

import com.lithium.flow.access.Access;
import com.lithium.flow.config.Config;
import com.lithium.flow.filer.CachedFiler;
import com.lithium.flow.filer.Filer;
import com.lithium.flow.filer.LocalFiler;
import com.lithium.flow.filer.Record;
import com.lithium.flow.filer.RecordPath;
import com.lithium.flow.shell.Shells;
import com.lithium.flow.shell.Shore;
import com.lithium.flow.util.Lazy;
import com.lithium.flow.util.Logs;
import com.lithium.flow.util.Measure;
import com.lithium.flow.util.Progress;
import com.lithium.flow.util.Threader;
import com.lithium.flow.vault.Vault;
import com.lithium.flow.vault.Vaults;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.slf4j.Logger;

import com.google.common.base.Splitter;

/**
 * @author Matt Ayres
 */
public class RunnerContext {
	private static final Logger log = Logs.getLogger();

	private final Config config;
	private final Filer filer;
	private final Vault vault;
	private final Access access;
	private final Shore shore;
	private final Threader syncThreader;
	private final Threader runThreader;
	private final String srcDir;
	private final String normalizedSrcDir;
	private final JarProvider jarProvider;

	private final Lazy<Local> local;

	private final Progress progress;
	private final Measure hostsMeasure;
	private final Measure jarsMeasure;
	private final Measure filesMeasure;
	private final Measure copiedMeasure;
	private final Measure deletedMeasure;

	private class Local {
		private final List<String> jars = new ArrayList<>();
		private final List<Record> records = new ArrayList<>();
		private final List<String> classpath = new ArrayList<>();

		public Local() throws IOException {
			String libDir = config.getString("lib.dir");
			String javaHome = System.getProperty("java.home").replaceAll("/jre$", "");

			for (String path : Splitter.on(File.pathSeparator).split(System.getProperty("java.class.path", ""))) {
				if (!path.startsWith(javaHome)) {
					log.debug("classpath: {}", path);

					if (path.endsWith(".jar") || path.endsWith(".xml")) {
						String name = RecordPath.getName(path);
						String destPath = libDir + "/" + name;
						classpath.add(destPath);
						jars.add(path);
					} else {
						classpath.add(path);
						filer.findRecords(path, 1).filter(Record::isFile).forEach(records::add);
					}
				}
			}
		}
	}

	public RunnerContext(@Nonnull Config config) throws IOException {
		this.config = checkNotNull(config);

		filer = new CachedFiler(new LocalFiler(), config);
		vault = Vaults.buildVault(config);
		access = Vaults.buildAccess(config, vault);
		shore = Shells.buildShore(config, access);
		syncThreader = new Threader(config.getInt("sync.threads"));
		runThreader = new Threader(config.getInt("run.threads"));
		srcDir = new File(config.getString("src.dir")).getCanonicalPath();
		normalizedSrcDir = normalize(srcDir);
		jarProvider = JarProvider.build(config, access, filer);

		local = new Lazy<>(Local::new).eager();

		progress = Progress.start(config);
		hostsMeasure = progress.counter("hosts");
		jarsMeasure = progress.counter("jars");
		filesMeasure = progress.counter("files").useForEta();
		copiedMeasure = progress.bandwidth("copied").useForEta();
		deletedMeasure = progress.counter("deleted");
	}

	public void close() throws IOException {
		syncThreader.finish();
		runThreader.finish();
		shore.close();
	}

	@Nonnull
	public List<String> getClasspath(@Nonnull String destDir) {
		Stream<String> classpath = local.get().classpath.stream();
		return classpath.map(path -> normalize(path).replace(normalizedSrcDir, destDir)).collect(toList());
	}

	@Nonnull
	public String normalize(@Nonnull String path) {
		return RecordPath.from(path).getPath().replaceFirst("^[A-Z]:", "");
	}

	@Nonnull
	public Filer getFiler() {
		return filer;
	}

	@Nonnull
	public Config getConfig() {
		return config;
	}

	@Nonnull
	public Vault getVault() {
		return vault;
	}

	@Nonnull
	public Access getAccess() {
		return access;
	}

	@Nonnull
	public Shore getShore() {
		return shore;
	}

	@Nonnull
	public Threader getSyncThreader() {
		return syncThreader;
	}

	@Nonnull
	public Threader getRunThreader() {
		return runThreader;
	}

	@Nonnull
	public String getSrcDir() {
		return srcDir;
	}

	@Nonnull
	public String getNormalizedSrcDir() {
		return normalizedSrcDir;
	}

	@Nonnull
	public JarProvider getJarProvider() {
		return jarProvider;
	}

	@Nonnull
	public List<String> getJars() {
		return local.get().jars;
	}

	@Nonnull
	public List<Record> getRecords(@Nonnull List<String> paths) throws IOException {
		List<Record> records = new ArrayList<>(local.get().records);
		for (String path : paths) {
			log.debug("path: {}", path);

			Record record = filer.getRecord(new File(path).getCanonicalPath());
			if (record.isDir()) {
				filer.findRecords(path, 1).filter(Record::isFile).forEach(records::add);
			} else {
				records.add(record);
			}
		}
		return records;
	}

	@Nonnull
	public Progress getProgress() {
		return progress;
	}

	@Nonnull
	public Measure getHostsMeasure() {
		return hostsMeasure;
	}

	@Nonnull
	public Measure getJarsMeasure() {
		return jarsMeasure;
	}

	@Nonnull
	public Measure getFilesMeasure() {
		return filesMeasure;
	}

	@Nonnull
	public Measure getCopiedMeasure() {
		return copiedMeasure;
	}

	@Nonnull
	public Measure getDeletedMeasure() {
		return deletedMeasure;
	}
}
