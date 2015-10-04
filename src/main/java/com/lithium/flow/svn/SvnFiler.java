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

package com.lithium.flow.svn;

import static com.google.common.base.Preconditions.checkNotNull;

import com.lithium.flow.filer.Filer;
import com.lithium.flow.filer.Record;
import com.lithium.flow.io.DataIo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.annotation.Nonnull;

import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.io.SVNRepository;

import com.google.common.collect.Lists;

/**
 * @author Matt Ayres
 */
public class SvnFiler implements Filer {
	static {
		SVNRepositoryFactoryImpl.setup();
	}

	private final SvnProvider svnProvider;
	private final long revision;
	private final URI uri;

	public SvnFiler(@Nonnull SvnProvider svnProvider) throws SVNException, URISyntaxException {
		this(svnProvider, -1);
	}

	public SvnFiler(@Nonnull SvnProvider svnProvider, long revision) {
		this.svnProvider = checkNotNull(svnProvider);
		this.revision = revision;
		uri = URI.create(svnProvider.getLocation().getURIEncodedPath());
	}

	@Override
	@Nonnull
	public URI getUri() {
		return uri;
	}

	@Override
	@Nonnull
	public Record getRecord(@Nonnull String path) throws IOException {
		SVNRepository repository = svnProvider.getRepository();
		try {
			SVNDirEntry entry = repository.info(path, revision);
			File file = new File(path);
			String parent = file.getParent() != null ? file.getParent() : "";
			if (entry != null) {
				return getRecord(entry, parent);
			} else {
				return new Record(getUri(), parent, file.getName(), 0,
						Record.NO_EXIST_SIZE, false);
			}
		} catch (SVNException e) {
			throw new IOException("failed to get file record: " + getFullPath(path), e);
		} finally {
			svnProvider.releaseRepository(repository);
		}
	}

	@Nonnull
	private Record getRecord(@Nonnull SVNDirEntry entry, @Nonnull String parent) {
		boolean dir = SVNNodeKind.DIR.equals(entry.getKind());
		long size = dir ? 0 : entry.getSize();
		return new Record(getUri(), parent, entry.getName(), entry.getDate().getTime(), size, dir);
	}

	@Override
	@Nonnull
	public List<Record> listRecords(@Nonnull String path) throws IOException {
		List<Record> records = Lists.newArrayList();
		if (getRecord(path).exists()) {
			SVNRepository repository = svnProvider.getRepository();
			try {
				repository.getDir(path, revision, new SVNProperties(), entry -> records.add(getRecord(entry, path)));
			} catch (SVNException e) {
				throw new IOException("failed to get file records: " + getFullPath(path), e);
			} finally {
				svnProvider.releaseRepository(repository);
			}
		}
		return records;
	}

	@Override
	@Nonnull
	public InputStream readFile(@Nonnull String path) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		SVNRepository repository = svnProvider.getRepository();
		try {
			repository.getFile(path, revision, new SVNProperties(), out);
		} catch (SVNException e) {
			throw new IOException("failed to read file: " + getFullPath(path), e);
		} finally {
			svnProvider.releaseRepository(repository);
		}
		return new ByteArrayInputStream(out.toByteArray());
	}

	@Nonnull
	private String getFullPath(@Nonnull String path) {
		return svnProvider.getLocation() + "/" + path;
	}

	@Override
	@Nonnull
	public OutputStream writeFile(@Nonnull String path) {
		throw new UnsupportedOperationException("writeFile not implemented yet");
	}

	@Override
	@Nonnull
	public DataIo openFile(@Nonnull String path, boolean write) throws IOException {
		throw new UnsupportedOperationException("openFile not implemented yet");
	}

	@Override
	public void setFileTime(@Nonnull String path, long time) {
		throw new UnsupportedOperationException("setFileTime not implemented yet");
	}

	@Override
	public void deleteFile(@Nonnull String path) {
		throw new UnsupportedOperationException("deleteFile not implemented yet");
	}

	@Override
	public void createDirs(@Nonnull String path) {
		throw new UnsupportedOperationException("createDirs not implemented yet");
	}

	@Override
	public void renameFile(@Nonnull String oldPath, @Nonnull String newPath) {
		throw new UnsupportedOperationException("renameFile not implemented yet");
	}

	@Override
	public void close() {
		svnProvider.close();
	}
}
