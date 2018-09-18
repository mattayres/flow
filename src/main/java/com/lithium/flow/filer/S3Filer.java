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

import com.lithium.flow.access.Access;
import com.lithium.flow.access.Prompt.Response;
import com.lithium.flow.access.Prompt.Type;
import com.lithium.flow.config.Config;
import com.lithium.flow.io.DataIo;
import com.lithium.flow.util.Lazy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.util.IOUtils;

/**
 * @author Matt Ayres
 */
public class S3Filer implements Filer {
	private final AmazonS3 s3;
	private final URI uri;
	private final String bucket;
	private final long partSize;
	private final File tempDir;
	private final boolean drainOnClose;
	private final ExecutorService service;
	private final boolean bypassCreateDirs;

	public S3Filer(@Nonnull Config config, @Nonnull Access access) {
		checkNotNull(config);
		checkNotNull(access);

		String url = config.getString("url");
		int index = url.indexOf("://");
		if (index > -1) {
			index = url.indexOf("/", index + 3);
			if (index > -1) {
				url = url.substring(0, index);
			}
		}
		uri = URI.create(url);

		bucket = uri.getHost();
		partSize = config.getInt("s3.partSize", 5 * 1024 * 1024);
		tempDir = new File(config.getString("s3.tempDir", System.getProperty("java.io.tmpdir")));
		drainOnClose = config.getBoolean("s3.drainOnClose", false);
		service = Executors.newFixedThreadPool(config.getInt("s3.threads", 1));
		bypassCreateDirs = config.getBoolean("s3.bypassCreateDirs", false);

		AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();

		ClientConfiguration cc = new ClientConfiguration();
		cc.setMaxErrorRetry(config.getInt("s3.maxErrorRetry", 3));
		cc.setConnectionTimeout((int) config.getTime("s3.connectionTimeout", "10s"));
		cc.setRequestTimeout((int) config.getTime("s3.requestTimeout", "0"));
		cc.setSocketTimeout((int) config.getTime("s3.socketTimeout", "50s"));
		builder.withClientConfiguration(cc);

		String region = config.getString("aws.region", null);
		String endpoint = config.getString("aws.endpoint", null);

		if (endpoint != null) {
			builder.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, region));
		} else if (region != null) {
			builder.withRegion(region);
		}

		builder.withChunkedEncodingDisabled(getBooleanOrNull(config, "s3.chunkedEncodingDisabled"));
		builder.withPathStyleAccessEnabled(getBooleanOrNull(config, "s3.pathStyleAccessEnabled"));

		String key = config.getString("aws.key", null);
		if (key != null) {
			AmazonS3Exception exception = null;
			int retries = config.getInt("aws.retries", 3);

			for (int i = 0; i < retries + 1; i++) {
				Response response = access.prompt(key + ".secret", key + ".secret: ", Type.MASKED);
				try {
					String secret = response.value();
					builder.withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(key, secret)));

					AmazonS3 testS3 = builder.build();
					testS3.listObjects(listObjectsRequest().withMaxKeys(1));
					response.accept();
					break;
				} catch (AmazonS3Exception e) {
					exception = e;
					response.reject();
				}
			}

			if (exception != null) {
				throw exception;
			}
		}

		s3 = builder.build();
	}

	@Nullable
	private Boolean getBooleanOrNull(@Nonnull Config config, @Nonnull String key) {
		return config.containsKey(key) ? config.getBoolean(key) : null;
	}

	@Override
	@Nonnull
	public URI getUri() {
		return uri;
	}

	@Override
	@Nonnull
	public List<Record> listRecords(@Nonnull String path) {
		String prefix = path.isEmpty() || path.equals("/") ? "" : keyForPath(path) + "/";
		ObjectListing listing = s3.listObjects(listObjectsRequest().withPrefix(prefix).withDelimiter("/"));

		List<Record> records = new ArrayList<>();

		do {
			for (String dir : listing.getCommonPrefixes()) {
				String name = dir.replaceFirst(prefix, "").replace("/", "");
				records.add(new Record(uri, RecordPath.from(path, name), 0, 0, true));
			}

			for (S3ObjectSummary summary : listing.getObjectSummaries()) {
				if (!summary.getKey().endsWith("/")) {
					String name = RecordPath.getName(summary.getKey());
					long time = summary.getLastModified().getTime();
					long size = summary.getSize();
					records.add(new Record(uri, RecordPath.from(path, name), time, size, false));
				}
			}

			listing = s3.listNextBatchOfObjects(listing);
		} while (listing.isTruncated());

		return records;
	}

	@Override
	@Nonnull
	public Record getRecord(@Nonnull String path) {
		ObjectListing listing = s3.listObjects(listObjectsRequest().withPrefix(keyForPath(path)));
		S3ObjectSummary summary = listing.getObjectSummaries().stream().findFirst().orElse(null);

		if (summary == null || !path.equals("/" + summary.getKey())) {
			return Record.noFile(uri, path);
		}

		long time = summary.getLastModified().getTime();
		long size = summary.getSize();
		boolean directory = summary.getKey().endsWith("/");
		return new Record(uri, RecordPath.from(path), time, size, directory);
	}

	@Override
	@Nonnull
	public InputStream readFile(@Nonnull String path) {
		InputStream in = s3.getObject(bucket, keyForPath(path)).getObjectContent();
		if (drainOnClose) {
			return new FilterInputStream(in) {
				@Override
				public void close() throws IOException {
					IOUtils.drainInputStream(in);
					super.close();
				}
			};
		} else {
			return in;
		}
	}

	@Override
	@Nonnull
	public OutputStream writeFile(@Nonnull String path) {
		String key = keyForPath(path);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		List<Future<PartETag>> futureTags = new ArrayList<>();
		Lazy<String> uploadId = new Lazy<>(
				() -> s3.initiateMultipartUpload(new InitiateMultipartUploadRequest(bucket, key)).getUploadId());

		return new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				baos.write(b);
				flip(partSize);
			}

			@Override
			public void write(@Nonnull byte[] b) throws IOException {
				baos.write(b);
				flip(partSize);
			}

			@Override
			public void write(@Nonnull byte[] b, int off, int len) throws IOException {
				baos.write(b, off, len);
				flip(partSize);
			}

			@Override
			public void close() throws IOException {
				if (futureTags.size() == 0) {
					InputStream in = new ByteArrayInputStream(baos.toByteArray());
					ObjectMetadata metadata = new ObjectMetadata();
					metadata.setContentLength(baos.size());
					s3.putObject(bucket, key, in, metadata);
				} else {
					flip(1);

					List<PartETag> tags = new ArrayList<>();
					for (Future<PartETag> futureTag : futureTags) {
						try {
							tags.add(futureTag.get());
						} catch (Exception e) {
							s3.abortMultipartUpload(new AbortMultipartUploadRequest(bucket, key, uploadId.get()));
							throw new IOException("failed to upload: " + path, e);
						}
					}

					s3.completeMultipartUpload(new CompleteMultipartUploadRequest(bucket, key, uploadId.get(), tags));
				}
			}

			private void flip(long minSize) throws IOException {
				if (baos.size() < minSize) {
					return;
				}

				File file = new File(tempDir, UUID.randomUUID().toString());
				file.deleteOnExit();

				OutputStream out = new FileOutputStream(file);
				out.write(baos.toByteArray());
				out.close();

				baos.reset();

				UploadPartRequest uploadRequest = new UploadPartRequest()
						.withUploadId(uploadId.get())
						.withBucketName(bucket)
						.withKey(key)
						.withPartNumber(futureTags.size() + 1)
						.withPartSize(file.length())
						.withFile(file);

				futureTags.add(service.submit(() -> {
					PartETag tag = s3.uploadPart(uploadRequest).getPartETag();
					if (!file.delete()) {
						throw new IOException("failed to delete: " + file.getPath());
					}
					return tag;
				}));
			}
		};
	}

	@Override
	@Nonnull
	public OutputStream appendFile(@Nonnull String path) {
		throw new UnsupportedOperationException();
	}

	@Override
	@Nonnull
	public DataIo openFile(@Nonnull String path, boolean write) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setFileTime(@Nonnull String path, long time) {
		String key = keyForPath(path);
		ObjectMetadata metadata = s3.getObjectMetadata(bucket, key);
		metadata.setLastModified(new Date(time));
		s3.copyObject(new CopyObjectRequest(bucket, key, bucket, key).withNewObjectMetadata(metadata));
	}

	@Override
	public void deleteFile(@Nonnull String path) {
		s3.deleteObject(bucket, keyForPath(path));
	}

	@Override
	public void renameFile(@Nonnull String oldPath, @Nonnull String newPath) {
		String oldKey = keyForPath(oldPath);
		String newKey = keyForPath(newPath);
		s3.copyObject(new CopyObjectRequest(bucket, oldKey, bucket, newKey));
		s3.deleteObject(bucket, oldKey);
	}

	@Override
	public void createDirs(@Nonnull String path) {
		if (!bypassCreateDirs) {
			InputStream in = new ByteArrayInputStream(new byte[0]);
			ObjectMetadata metadata = new ObjectMetadata();
			metadata.setContentLength(0);
			s3.putObject(bucket, keyForPath(path) + "/", in, metadata);
		}
	}

	@Nonnull
	private String keyForPath(@Nonnull String path) {
		return path.startsWith("/") ? path.substring(1) : path;
	}

	@Nonnull
	private ListObjectsRequest listObjectsRequest() {
		return new ListObjectsRequest().withBucketName(bucket);
	}

	@Override
	public void close() {
	}
}
