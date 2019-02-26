package com.gentics.mesh.storage.s3;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionException;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.NotImplementedException;
import org.reactivestreams.Publisher;

import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.storage.AbstractBinaryStorage;
import com.gentics.mesh.util.RxUtil;

import hu.akarnokd.rxjava2.interop.CompletableInterop;
import hu.akarnokd.rxjava2.interop.FlowableInterop;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.file.AsyncFile;
import io.vertx.reactivex.core.file.FileProps;
import io.vertx.reactivex.core.file.FileSystem;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Singleton
public class S3BinaryStorage extends AbstractBinaryStorage {

	private static final Logger log = LoggerFactory.getLogger(S3BinaryStorage.class);

	private S3AsyncClient client;

	private S3StorageOptions options;

	private FileSystem fs;

	private String bucketName;

	@Inject
	public S3BinaryStorage(S3StorageOptions options, Vertx rxVertx) {
		this.options = options;
		this.fs = rxVertx.fileSystem();
		this.bucketName = options.getBucketName();
		init();
	}

	private void init() {
		AwsCredentials credentials = AwsBasicCredentials.create(options.getAccessId(), options.getAccessKey());
		client = S3AsyncClient.builder()
			.region(Region.of(options.getRegion()))
			.endpointOverride(URI.create(options.getUrl()))
			.credentialsProvider(StaticCredentialsProvider.create(credentials))
			.build();

		createBucket(bucketName);
	}

	private void createBucket(String bucketName) {
		HeadBucketRequest headRequest = HeadBucketRequest.builder()
			.bucket(bucketName)
			.build();
		CreateBucketRequest createRequest = CreateBucketRequest.builder()
			.bucket(bucketName)
			.build();

		Single<HeadBucketResponse> bucketHead = SingleInterop.fromFuture(client.headBucket(headRequest));
		bucketHead.map(e -> e != null).onErrorResumeNext(e -> {
			if (e instanceof CompletionException && e.getCause() != null && e.getCause() instanceof NoSuchBucketException) {
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
	public Single<Boolean> exists(BinaryGraphField field) {
		// TODO should this be uuid instead of id?
		String id = field.getBinary().getSHA512Sum();
		HeadObjectRequest request = HeadObjectRequest.builder()
			.bucket(bucketName)
			.key(id)
			.build();

		return SingleInterop.fromFuture(client.headObject(request)).map(r -> r != null).onErrorResumeNext(e -> {
			if (e instanceof CompletionException && e.getCause() != null && e.getCause() instanceof NoSuchKeyException) {
				return Single.just(false);
			} else {
				return Single.error(e);
			}
		}).doOnError(e -> {
			log.error("Error while checking for field {" + id + "}", id, e);
		});
	}

	@Override
	public Flowable<Buffer> read(String uuid) {
		return Flowable.defer(() -> {
			if (log.isDebugEnabled()) {
				log.debug("Loading data for hash {" + uuid + "}");
			}
			GetObjectRequest request = GetObjectRequest.builder()
				.bucket(options.getBucketName())
				.key(uuid)
				.build();
			return FlowableInterop.fromFuture(client.getObject(request, AsyncResponseTransformer.toBytes()));
		}).map(f -> Buffer.buffer(f.asByteArray()));
	}

	@Override
	public Completable storeInTemp(String sourceFilePath, String temporaryId) {
		Single<AsyncFile> open = fs.rxOpen(sourceFilePath, new OpenOptions());
		Single<FileProps> props = fs.rxProps(sourceFilePath);
		return Single.zip(open, props, (file, info) -> {
			Flowable<Buffer> stream = RxUtil.toBufferFlow(file);
			long size = info.size();
			return storeInTemp(stream, size, temporaryId);
		}).doOnError(e -> {
			log.error("Error while storing file {} in temp with id {}", sourceFilePath, temporaryId, e);
		}).toCompletable();
	}

	@Override
	public Completable storeInTemp(Flowable<Buffer> stream, long size, String temporaryId) {
		PutObjectRequest request = PutObjectRequest.builder()
			.bucket(bucketName)
			.key(temporaryId)
			.contentLength(size)
			.build();

		Publisher<ByteBuffer> publisher = stream.map(b -> ByteBuffer.wrap(b.getBytes()));
		return CompletableInterop.fromFuture(client.putObject(request, AsyncRequestBody.fromPublisher(publisher)));
	}

	@Override
	public Completable delete(String uuid) {
		DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
			.key(uuid)
			.bucket(bucketName)
			.build();

		Single<DeleteObjectResponse> deleteAction = SingleInterop.fromFuture(client.deleteObject(deleteRequest));
		return deleteAction.toCompletable();
	}

	@Override
	public Buffer readAllSync(String uuid) {
		throw new NotImplementedException("Not implemented for S3 storage");
	}

	@Override
	public Completable moveInPlace(String uuid, String temporaryId) {
		CopyObjectRequest copyRequest = CopyObjectRequest.builder()
			.bucket(bucketName)
			.key(uuid)
			.copySource(bucketName + "/" + temporaryId)
			.build();

		Completable copy = CompletableInterop.fromFuture(client.copyObject(copyRequest))
			.doOnError(e -> {
				log.error("Error while copying {} to {} in bucket {}", uuid, temporaryId, bucketName);
			});

		DeleteObjectRequest deleObjectRequest = DeleteObjectRequest.builder()
			.bucket(bucketName)
			.key(temporaryId)
			.build();
		Completable delete = CompletableInterop.fromFuture(client.deleteObject(deleObjectRequest))
			.doOnError(e -> {
				log.error("Error while deleting temporary file {}", temporaryId, e);
			});
		return copy.andThen(delete);
	}

	@Override
	public Completable purgeTemporaryUpload(String temporaryId) {
		return Completable.complete();
	}

	@Override
	public InputStream openBlockingStream(String uuid) throws IOException {
		return null;
	}
}
