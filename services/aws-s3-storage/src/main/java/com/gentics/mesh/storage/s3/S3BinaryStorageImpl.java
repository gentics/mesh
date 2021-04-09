package com.gentics.mesh.storage.s3;

import com.gentics.mesh.core.rest.node.field.s3binary.S3RestResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.binary.HibBinaryField;
import com.gentics.mesh.core.rest.node.field.s3binary.S3RestResponse;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.S3Options;
import com.gentics.mesh.storage.S3BinaryStorage;
import hu.akarnokd.rxjava2.interop.FlowableInterop;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.file.FileSystem;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Initial S3 Storage implementation.
 */
@Singleton
public class S3BinaryStorageImpl implements S3BinaryStorage {

	private static final Logger log = LoggerFactory.getLogger(S3BinaryStorageImpl.class);

	private S3AsyncClient client;

	private S3Presigner presigner;

	private S3Options options;

	private final Vertx rxVertx;

	private FileSystem fs;

	@Inject
	public S3BinaryStorageImpl(MeshOptions options, Vertx rxVertx) {
		this.options = options.getS3Options();
		this.rxVertx = rxVertx;
		this.fs = rxVertx.fileSystem();
		init();
	}

	private void init() {
		AwsCredentials credentials = AwsBasicCredentials.create(options.getAccessKeyId(), options.getSecretAccessKey());
		System.setProperty("aws.accessKeyId", options.getAccessKeyId());
		System.setProperty("aws.secretAccessKey", options.getSecretAccessKey());

		S3AsyncClientBuilder clientBuilder = S3AsyncClient.builder();

		// Endpoint override is optional
		if (options.getEndpoint() != null) {
			clientBuilder.endpointOverride(URI.create(options.getEndpoint()));
		}

		client = clientBuilder.region(Region.of(options.getRegion()))
				.credentialsProvider(StaticCredentialsProvider.create(credentials))
				.build();

		S3Presigner.Builder presignerBuilder = S3Presigner.builder();

		if (options.getEndpoint() != null) {
			presignerBuilder.endpointOverride(URI.create(options.getEndpoint()));
		}

		this.presigner = presignerBuilder.region(Region.of(options.getRegion()))
				.credentialsProvider(StaticCredentialsProvider.create(credentials))
				.build();

		String bucketName = options.getBucket();
		createBucket(bucketName);
	}

	@Override
	public void createBucket(String bucketName) {
		HeadBucketRequest headRequest = HeadBucketRequest.builder()
				.bucket(bucketName)
				.build();
		CreateBucketRequest createRequest = CreateBucketRequest.builder()
				.bucket(bucketName)
				.build();
		Single<HeadBucketResponse> bucketHead = SingleInterop.fromFuture(client.headBucket(headRequest));
		bucketHead.map(e -> e != null)
				.onErrorResumeNext(e -> {
					//try to create new bucket if bucket cannot be found
					if (e instanceof CompletionException && e.getCause() != null) {
						return SingleInterop.fromFuture(client.createBucket(createRequest)).map(r -> r != null);
					} else {
						return Single.error(e);
					}
				}).subscribe(b -> {
			log.info("Created bucket {}", bucketName);
		}, e -> {
			log.error("Error while creating bucket", e);
		});
	}

	@Override
	public Single<S3RestResponse> createPresignedUrl(String nodeUuid, String fieldName) {
		String bucketName = options.getBucket();
		int expirationTimeUpload = options.getExpirationTimeUpload();
		String objectKey = nodeUuid + "/" + fieldName;

		PresignedPutObjectRequest presignedRequest =
				presigner.presignPutObject(r -> r.signatureDuration(Duration.ofSeconds(expirationTimeUpload))
						.putObjectRequest(por -> por.bucket(bucketName).key(objectKey)));

		if (log.isDebugEnabled()) {
			log.debug("Creating presigned URL for nodeUuid '{}' and fieldName '{}'", nodeUuid, fieldName);
		}

		S3RestResponse s3RestResponse = new S3RestResponse(presignedRequest.url().toString(), presignedRequest.httpRequest().method().toString(), presignedRequest.signedHeaders());
		presigner.close();
		return Single.just(s3RestResponse);
	}

	@Override
		public Flowable<Buffer> read(String objectKey) {
			return Flowable.defer(() -> {
				if (log.isDebugEnabled()) {
					log.debug("Loading data for uuid {" + objectKey + "}");
				}
				GetObjectRequest request = GetObjectRequest.builder()
						.bucket(options.getBucket())
						.key(objectKey)
						.build();
				return FlowableInterop.fromFuture(client.getObject(request, AsyncResponseTransformer.toBytes()));
			}).map(f ->{
				System.out.println(f.asByteArray().length);
				return	Buffer.buffer(f.asByteArray());
			});
	}

	@Override
	public Completable delete(String uuid) {
		return Completable.complete();
	}
}
