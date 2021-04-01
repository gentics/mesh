package com.gentics.mesh.storage.s3;

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
import com.gentics.mesh.storage.AbstractBinaryStorage;
import com.gentics.mesh.util.RxUtil;

import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.OpenOptions;
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
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

/**
 * Initial S3 Storage implementation.
 */
@Singleton
public class S3BinaryStorage extends AbstractBinaryStorage {

	private static final Logger log = LoggerFactory.getLogger(S3BinaryStorage.class);

	private S3AsyncClient client;

	private S3Presigner presigner;

	private S3Options options;

	private final Vertx rxVertx;

	private FileSystem fs;

	@Inject
	public S3BinaryStorage(MeshOptions options, Vertx rxVertx) {
		this.options = options.getS3Options();
		this.rxVertx = rxVertx;
		this.fs = rxVertx.fileSystem();
		init();
	}

	private void init() {
		AwsCredentials credentials = AwsBasicCredentials.create(options.getAccessKeyId(), options.getSecretAccessKey());
		System.setProperty("aws.accessKeyId", options.getAccessKeyId());
		System.setProperty("aws.secretAccessKey", options.getSecretAccessKey());

		client = S3AsyncClient.builder()
				.region(Region.of(options.getRegion()))
				.credentialsProvider(StaticCredentialsProvider.create(credentials))
				.build();

		this.presigner = S3Presigner.builder()
				.region(Region.of(options.getRegion()))
				.credentialsProvider(StaticCredentialsProvider.create(credentials))
				.build();

		String bucketName = options.getBucket();
		createBucket(bucketName);
	}

	@Override
	public boolean exists(HibBinaryField field) {
		String id = field.getBinary().getSHA512Sum();
		// NoSuchKeyException
		try {
			HeadObjectResponse headResponse = client.headObject(HeadObjectRequest.builder()
					.bucket(options.getBucket())
					.key(id)
					.build()).get();
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

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

	public S3RestResponse createPresignedUrl(String nodeUuid, String fieldName) {
		String bucketName = options.getBucket();
		int expirationTimeUpload = options.getExpirationTimeUpload();
		String objectKey = nodeUuid + "/" + fieldName;

		PresignedPutObjectRequest presignedRequest =
				presigner.presignPutObject(r -> r.signatureDuration(Duration.ofSeconds(expirationTimeUpload))
						.putObjectRequest(por -> por.bucket(bucketName).key(objectKey)));

		System.out.println("Pre-signed URL to upload a file to: " +
				presignedRequest.url());
		System.out.println("Which HTTP method needs to be used when uploading a file: " +
				presignedRequest.httpRequest().method());
		System.out.println("Which headers need to be sent with the upload: " +
				presignedRequest.signedHeaders());
		S3RestResponse s3RestResponse = new S3RestResponse(presignedRequest.url().toString(), presignedRequest.httpRequest().method().toString(), presignedRequest.signedHeaders());
		presigner.close();
		return s3RestResponse;
	}

	@Override
	public Flowable<Buffer> read(String hashsum) {
		return Flowable.generate(sub -> {
			if (log.isDebugEnabled()) {
				log.debug("Loading data for hash {" + hashsum + "}");
			}
			// GetObjectRequest rangeObjectRequest = new GetObjectRequest(options.getBucketName(), hashsum);
			GetObjectRequest request = GetObjectRequest.builder().bucket(options.getBucket()).build();
			client.getObject(request, new AsyncResponseTransformer<GetObjectResponse, String>() {

				@Override
				public CompletableFuture<String> prepare() {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public void onResponse(GetObjectResponse response) {
					// TODO Auto-generated method stub

				}

				@Override
				public void onStream(SdkPublisher<ByteBuffer> publisher) {
					// TODO Auto-generated method stub

				}

				@Override
				public void exceptionOccurred(Throwable error) {
					// TODO Auto-generated method stub

				}
			});

			// try (InputStream stream = objectPortion.getObjectContent()) {
			//
			// if (log.isDebugEnabled()) {
			// log.debug("Printing bytes retrieved:");
			// displayTextInputStream(stream);
			// }
			// }
			// sub.onComplete();
		});
	}

	@Override
	public Completable storeInTemp(String sourceFilePath, String temporaryId) {
		return fs.rxOpen(sourceFilePath, new OpenOptions()).flatMapCompletable(asyncFile -> {
			Flowable<Buffer> stream = RxUtil.toBufferFlow(asyncFile);
			return storeInTemp(stream, temporaryId);
		}).doOnError(e -> {
			log.error("Error while storing file {} in temp with id {}", sourceFilePath, temporaryId, e);
		});
	}

	@Override
	public Completable storeInTemp(Flowable<Buffer> stream, String temporaryId) {
		return Completable.create(sub -> {
			PutObjectRequest request = PutObjectRequest.builder()
					.bucket(options.getBucket())
					.key(temporaryId)
					.build();

			/*
			 * client.putObject(request, new AsyncRequestBody() {
			 *
			 * @Override public void subscribe(Subscriber<? super ByteBuffer> s) { stream.map(Buffer::getByteBuf).map(ByteBuf::nioBuffer).subscribe(s); }
			 *
			 * @Override public Optional<Long> contentLength() { // return Optional.from(10L); return null; } });
			 */
		});

		// try {
		// if (log.isDebugEnabled()) {
		// log.debug("Uploading {" + hashsum + "} to S3.");
		// }
		// try (InputStream ins = RxUtil.toInputStream(stream, new io.vertx.reactivex.core.Vertx(vertx))) {
		// ObjectMetadata metaData = new ObjectMetadata();
		// metaData.setContentLength(90);
		// s3Client.putObject(new PutObjectRequest(options.getBucketName(), hashsum, ins, metaData));
		// sub.onComplete();
		// }
		// } catch (AmazonServiceException ase) {
		// log.debug(
		// "Caught an AmazonServiceException, which means your request made it to Amazon S3, but was rejected with an error response for some reason.");
		// log.debug("Error Message: " + ase.getMessage());
		// log.debug("HTTP Status Code: " + ase.getStatusCode());
		// log.debug("AWS Error Code: " + ase.getErrorCode());
		// log.debug("Error Type: " + ase.getErrorType());
		// log.debug("Request ID: " + ase.getRequestId());
		// sub.onError(ase);
		// } catch (AmazonClientException ace) {
		// log.debug("Caught an AmazonClientException, which " + "means the client encountered " + "an internal error while trying to "
		// + "communicate with S3, " + "such as not being able to access the network.");
		// log.debug("Error Message: " + ace.getMessage());
		// sub.onError(ace);
		// }
	}

	@Override
	public Completable delete(String uuid) {
		return Completable.complete();
	}

	@Override
	public Buffer readAllSync(String uuid) {
		// TODO implement
		return null;
	}

	@Override
	public Completable moveInPlace(String uuid, String temporaryId) {
		return Completable.complete();
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
