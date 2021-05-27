package com.gentics.mesh.storage.s3;

import static java.util.Objects.isNull;

import java.io.File;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.rest.node.field.s3binary.S3RestResponse;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.S3Options;
import com.gentics.mesh.storage.S3BinaryStorage;

import hu.akarnokd.rxjava2.interop.CompletableInterop;
import hu.akarnokd.rxjava2.interop.FlowableInterop;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.core.http.impl.MimeMapping;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.buffer.Buffer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CORSConfiguration;
import software.amazon.awssdk.services.s3.model.CORSRule;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutBucketCorsRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

/**
 * Initial S3 Storage implementation.
 */
@Singleton
public class S3BinaryStorageImpl implements S3BinaryStorage {

	private static final Logger log = LoggerFactory.getLogger(S3BinaryStorageImpl.class);

	private S3AsyncClient client;

	private S3Presigner presigner;

	private S3Options options;

	@Inject
	public S3BinaryStorageImpl(MeshOptions options) {
		this.options = options.getS3Options();
	}

    public S3BinaryStorageImpl(MeshOptions options, S3AsyncClient client) {
        this.options = options.getS3Options();
        this.client = client;
    }

    public S3BinaryStorageImpl() {
    }

    public void init() {
        if (isNull(options) || isNull(options.getAccessKeyId()) || isNull(options.getSecretAccessKey())) throw new RuntimeException();
        try {
            AwsCredentials credentials = AwsBasicCredentials.create(options.getAccessKeyId(), options.getSecretAccessKey());
            System.setProperty("aws.accessKeyId", options.getAccessKeyId());
            System.setProperty("aws.secretAccessKey", options.getSecretAccessKey());

		S3AsyncClientBuilder clientBuilder = S3AsyncClient.builder();

            // Endpoint override is optional
            if (options.getEndpoint() != null) {
                clientBuilder.endpointOverride(URI.create(options.getEndpoint()));
                S3Configuration config = S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .checksumValidationEnabled(false)
                        .build();
                clientBuilder.serviceConfiguration(config);
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

            //create buckets with CORS configuration, so that UI can request images directly.
            String bucketName = options.getBucket();
            String cacheBucketName = options.getS3CacheOptions().getBucket();
			createBucket(bucketName).blockingGet();
			createBucket(cacheBucketName).blockingGet();
            CORSRule corsRule = CORSRule.builder().allowedHeaders("*").allowedMethods("GET", "PUT", "POST", "DELETE").allowedOrigins("*").build();

            CORSConfiguration corsConfiguration = CORSConfiguration.builder().corsRules(corsRule).build();
            PutBucketCorsRequest putBucketCorsRequest = PutBucketCorsRequest.builder().bucket(bucketName).corsConfiguration(corsConfiguration).build();
            PutBucketCorsRequest putBucketCorsRequestCache = PutBucketCorsRequest.builder().bucket(cacheBucketName).corsConfiguration(corsConfiguration).build();
            SingleInterop.fromFuture(client.putBucketCors(putBucketCorsRequest)).blockingGet();
            SingleInterop.fromFuture(client.putBucketCors(putBucketCorsRequestCache)).blockingGet();
        } catch (Exception ex) {
            log.error("Error thrown when creating init and creating buckets: " + ex.getMessage());
        }
    }

    @Override
    public Single<Boolean> createBucket(String bucketName) {
		if(isNull(client)){
			init();
		}
        HeadBucketRequest headRequest = HeadBucketRequest.builder()
                .bucket(bucketName)
                .build();
        CreateBucketRequest createRequest = CreateBucketRequest.builder()
                .bucket(bucketName)
                .build();
        Single<HeadBucketResponse> bucketHead = SingleInterop.fromFuture(client.headBucket(headRequest));
        return bucketHead.map(e -> e != null)
                .onErrorResumeNext(e -> {
                    //try to create new bucket if bucket cannot be found
                    if (e instanceof CompletionException && e.getCause() != null) {
                        return SingleInterop.fromFuture(client.createBucket(createRequest)).map(r -> r != null);
                    } else {
                        return Single.error(e);
                    }
                });
    }

    @Override
    public Single<S3RestResponse> createUploadPresignedUrl(String bucketName, String nodeUuid, String fieldName, String nodeVersion, boolean isCache) {
		if(isNull(client)){
			init();
		}
		int expirationTimeUpload;
        //we need to establish a fixed expiration time upload for the cache
        if (isCache && options.getS3CacheOptions().getExpirationTimeUpload() > 0)
            expirationTimeUpload = options.getS3CacheOptions().getExpirationTimeUpload();
        else {
            expirationTimeUpload = options.getExpirationTimeUpload();
        }
        String objectKey = nodeUuid + "/" + fieldName;

		PresignedPutObjectRequest presignedRequest =
				presigner.presignPutObject(r -> r.signatureDuration(Duration.ofSeconds(expirationTimeUpload))
						.putObjectRequest(por -> por.bucket(bucketName).key(objectKey)));

		if (log.isDebugEnabled()) {
			log.debug("Creating presigned URL for nodeUuid '{}' and fieldName '{}'", nodeUuid, fieldName);
		}

		S3RestResponse s3RestResponse = new S3RestResponse(presignedRequest.url().toString(), presignedRequest.httpRequest().method().toString(), presignedRequest.signedHeaders());
		s3RestResponse.setVersion(nodeVersion);
		presigner.close();
		return Single.just(s3RestResponse);
	}

	@Override
	public Single<S3RestResponse> createDownloadPresignedUrl(String bucket, String objectKey, boolean isCache) {
		if(isNull(client)){
			init();
		}
		int expirationTimeDownload;
		if (isCache) expirationTimeDownload = options.getS3CacheOptions().getExpirationTimeDownload();
		else {
			expirationTimeDownload = options.getExpirationTimeDownload();
		}
		GetObjectRequest getObjectRequest =
				GetObjectRequest.builder()
						.bucket(bucket)
						.key(objectKey)
						.build();

		GetObjectPresignRequest getObjectPresignRequest =
				GetObjectPresignRequest.builder()
						.signatureDuration(Duration.ofSeconds(expirationTimeDownload))
						.getObjectRequest(getObjectRequest)
						.build();

		// Generate the presigned request
		PresignedGetObjectRequest presignedGetObjectRequest =
				presigner.presignGetObject(getObjectPresignRequest);

		// Log the presigned URL
		if (log.isDebugEnabled()) {
			log.debug("Presigned URL: '{}'", presignedGetObjectRequest.url());
		}

		String host = presignedGetObjectRequest.url().toString();
		SdkHttpMethod method = presignedGetObjectRequest.httpRequest().method();
		Map<String, List<String>> headers = presignedGetObjectRequest.httpRequest().headers();
		S3RestResponse s3RestResponse = new S3RestResponse(host, method.toString(), headers);
		return Single.just(s3RestResponse);
	}

	@Override
	public Flowable<Buffer> read(String bucketName, String objectKey) {
		if(isNull(client)){
			init();
		}
		return Flowable.defer(() -> {
			if (log.isDebugEnabled()) {
				log.debug("Loading data for uuid {" + objectKey + "}");
			}
			GetObjectRequest request = GetObjectRequest.builder()
					.bucket(bucketName)
					.key(objectKey)
					.build();
			return FlowableInterop.fromFuture(client.getObject(request, AsyncResponseTransformer.toBytes()));
		}).map(f -> Buffer.buffer(f.asByteArray()));
	}

    @Override
    public Single<S3RestResponse> uploadFile(String bucket, String objectKey, File file, boolean isCache) {
		if(isNull(client)){
			init();
		}
		String mimeTypeForFilename = MimeMapping.getMimeTypeForFilename(file.getName());

		PutObjectRequest objectRequest = PutObjectRequest.builder()
				.bucket(bucket)
				.key(objectKey)
				.contentType(mimeTypeForFilename)
				.build();
		String[] split = objectKey.split("/");

        // Put the object into the bucket
        Completable completable = CompletableInterop.fromFuture(client.putObject(objectRequest,
                AsyncRequestBody.fromFile(file)
        ));
        return completable
                .andThen(createUploadPresignedUrl(bucket, split[0], split[1], null, isCache))
                .doOnError(err -> Single.error(err));
    }

	@Override
	public Single<Boolean> exists(String bucket, String objectKey) {
		if(isNull(client)){
			init();
		}
		HeadObjectRequest request = HeadObjectRequest.builder()
				.bucket(bucket)
				.key(objectKey)
				.build();

		return SingleInterop.fromFuture(client.headObject(request)).map(r -> r != null).onErrorResumeNext(e -> {
			if (e instanceof CompletionException && e.getCause() != null && e.getCause() instanceof NoSuchKeyException) {
				return Single.just(false);
			} else {
				return Single.error(e);
			}
		}).doOnError(e -> {
			log.error("Error while checking for field {" + objectKey + "}", objectKey, e);
		});
	}

    @Override
    public Single<Boolean> exists(String bucketName) {
		if(isNull(client)){
			init();
		}
		HeadBucketRequest headRequest = HeadBucketRequest.builder()
                .bucket(bucketName)
                .build();
        return SingleInterop.fromFuture(client.headBucket(headRequest)).map(e -> e != null).onErrorResumeNext(e -> {
            if (e instanceof CompletionException && e.getCause() != null && e.getCause() instanceof NoSuchBucketException) {
                return Single.just(false);
            } else {
                return Single.error(e);
            }
        }).doOnError(e -> {
            log.error("Error while checking for bucket {" + bucketName + "}", bucketName, e);
        });
    }

    @Override
    public Completable delete(String bucket, String s3ObjectKey) {
		if(isNull(client)){
			init();
		}
		DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .key(s3ObjectKey)
                .bucket(bucket)
                .build();

		Completable deleteAction = Completable.fromFuture(client.deleteObject(deleteRequest));
		return deleteAction;
	}

	@Override
	public Completable delete(String s3ObjectKey) {
		if(isNull(client)){
			init();
		}
		return delete(options.getBucket(), s3ObjectKey);
	}
}
