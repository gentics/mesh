package com.gentics.mesh.storage.s3;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.binary.HibBinaryField;
import com.gentics.mesh.storage.AbstractBinaryStorage;
import com.gentics.mesh.util.RxUtil;

import io.reactivex.Completable;
import io.reactivex.Flowable;
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
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Initial S3 Storage implementation.
 */
@Singleton
public class S3BinaryStorage extends AbstractBinaryStorage {

	private static final Logger log = LoggerFactory.getLogger(S3BinaryStorage.class);

	private S3AsyncClient client;

	private S3StorageOptions options;

	private final Vertx rxVertx;

	private FileSystem fs;

	@Inject
	public S3BinaryStorage(S3StorageOptions options, Vertx rxVertx) {
		this.options = options;
		this.rxVertx = rxVertx;
		this.fs = rxVertx.fileSystem();
		init();
	}

	private void init() {
		AwsCredentials credentials = AwsBasicCredentials.create(options.getAccessId(), options.getAccessKey());
		// ClientConfiguration clientConfiguration = new ClientConfiguration();
		// clientConfiguration.setSignerOverride("AWSS3V4SignerType");

		// ClientAsyncHttpConfiguration asyncHttpConfiguration = ClientAsyncHttpConfiguration.builder().build();
		// S3AdvancedConfiguration advancedConfiguration = S3AdvancedConfiguration.builder().build();

		// DefaultCredentialsProvider.create();
		System.setProperty("aws.accessKeyId", options.getAccessId());
		System.setProperty("aws.secretAccessKey", options.getAccessKey());

		client = S3AsyncClient.builder()
			// .advancedConfiguration(advancedConfiguration)
			// .asyncHttpConfiguration(asyncHttpConfiguration)
			.region(Region.of(options.getRegion()))
			.endpointOverride(URI.create(options.getUrl()))
			.credentialsProvider(StaticCredentialsProvider.create(credentials))
			.build();

		String bucketName = options.getBucketName();
		// try {
		// HeadBucketResponse response = client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build()).get();
		// } catch (InterruptedException | ExecutionException e) {
		// TODO Auto-generated catch block
		// e.printStackTrace();
		// } catch (NoSuchKeyException e) {
		try {
			CreateBucketResponse response2 = client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build()).get();
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// }

		// s3Client = AmazonS3ClientBuilder
		// .standard()
		// .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(options.getUrl(), options.getRegion()))
		// .withPathStyleAccessEnabled(true)
		// .withClientConfiguration(clientConfiguration)
		// .withCredentials(new AWSStaticCredentialsProvider(credentials))
		// .build();
		//
		// String bucketName = options.getBucketName();
		// if (!s3Client.doesBucketExist(bucketName)) {
		// log.info("Did not find bucket {" + bucketName + "}. Creating it...");
		// s3Client.createBucket(new CreateBucketRequest(bucketName));
		// }

	}

	@Override
	public boolean exists(HibBinaryField field) {
		String id = field.getBinary().getSHA512Sum();
		// NoSuchKeyException
		try {
			HeadObjectResponse headResponse = client.headObject(HeadObjectRequest.builder()
				.bucket(options.getBucketName())
				.key(id)
				.build()).get();
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;

	}

	@Override
	public Flowable<Buffer> read(String hashsum) {
		return Flowable.generate(sub -> {
			if (log.isDebugEnabled()) {
				log.debug("Loading data for hash {" + hashsum + "}");
			}
			// GetObjectRequest rangeObjectRequest = new GetObjectRequest(options.getBucketName(), hashsum);
			GetObjectRequest request = GetObjectRequest.builder().bucket(options.getBucketName()).build();
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
				.bucket(options.getBucketName())
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
