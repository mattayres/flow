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

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nonnull;

/**
 * @author Matt Ayres
 */
public class FileDiff {
	private final Filer srcFiler;
	private final Filer destFiler;
	private final String srcPath;
	private final String destPath;
	private final long srcTime;
	private final long srcSize;

	public FileDiff(@Nonnull Filer srcFiler, @Nonnull Filer destFiler,
			@Nonnull String srcPath, @Nonnull String destPath, long srcTime, long srcSize) {
		this.srcFiler = checkNotNull(srcFiler);
		this.destFiler = checkNotNull(destFiler);
		this.srcPath = checkNotNull(srcPath);
		this.destPath = checkNotNull(destPath);
		this.srcTime = srcTime;
		this.srcSize = srcSize;
	}

	@Nonnull
	public Filer getSrcFiler() {
		return srcFiler;
	}

	@Nonnull
	public Filer getDestFiler() {
		return destFiler;
	}

	@Nonnull
	public String getSrcPath() {
		return srcPath;
	}

	@Nonnull
	public String getDestPath() {
		return destPath;
	}

	public long getSrcTime() {
		return srcTime;
	}

	public long getSrcSize() {
		return srcSize;
	}

	public FileDiff withDestPath(String destPath2) {
		return new FileDiff(srcFiler, destFiler, srcPath, destPath2, srcTime, srcSize);
	}
}
