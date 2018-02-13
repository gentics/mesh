package com.gentics.mesh.storage.s3;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.storage.AbstractBinaryStorage;

import io.netty.buffer.ByteBuf;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import software.amazon.awssdk.core.async.AsyncRequestProvider;
import software.amazon.awssdk.core.async.AsyncResponseHandler;
import software.amazon.awssdk.core.auth.AwsCredentials;
import software.amazon.awssdk.core.auth.StaticCredentialsProvider;
import software.amazon.awssdk.core.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class S3BinaryStorage extends AbstractBinaryStorage {

	private static final Logger log = LoggerFactory.getLogger(S3BinaryStorage.class);

	private S3AsyncClient client;

	private S3StorageOptions options;

	private Vertx vertx;

	public S3BinaryStorage(S3StorageOptions options, Vertx vertx) {
		this.options = options;
		this.vertx = vertx;
		init();
	}

	private void init() {
		AwsCredentials credentials = AwsCredentials.create(options.getAccessId(), options.getAccessKey());
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
//		try {
			//HeadBucketResponse response = client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build()).get();
//		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (NoSuchKeyException e) {
			try {
				CreateBucketResponse response2 = client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build()).get();
			} catch (InterruptedException | ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
//		}

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
	public boolean exists(BinaryGraphField field) {
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
	public Observable<Buffer> read(String hashsum) {
		return Observable.create(sub -> {
			if (log.isDebugEnabled()) {
				log.debug("Loading data for hash {" + hashsum + "}");
			}
			// GetObjectRequest rangeObjectRequest = new GetObjectRequest(options.getBucketName(), hashsum);
			GetObjectRequest request = GetObjectRequest.builder().bucket(options.getBucketName()).build();
			client.getObject(request, new AsyncResponseHandler<GetObjectResponse, String>() {

				@Override
				public void responseReceived(GetObjectResponse response) {
				}

				@Override
				public void onStream(Publisher<ByteBuffer> publisher) {
				}

				@Override
				public void exceptionOccurred(Throwable throwable) {
				}

				@Override
				public String complete() {
					return null;
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
	public Completable store(Observable<Buffer> stream, String hashsum) {
		return Completable.create(sub -> {
			PutObjectRequest request = PutObjectRequest.builder()
				.bucket(options.getBucketName())
				.key(hashsum)
				.build();

			client.putObject(request, new AsyncRequestProvider() {
				@Override
				public void subscribe(Subscriber<? super ByteBuffer> s) {
					stream.toFlowable(BackpressureStrategy.BUFFER).map(Buffer::getByteBuf).map(ByteBuf::nioBuffer).subscribe(s);
				}

				@Override
				public long contentLength() {
					// TODO Auto-generated method stub
					return 10;
				}
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
		});
	}

	@Override
	public Completable delete(String uuid) {
		// TODO Auto-generated method stub
		return null;
	}

}
