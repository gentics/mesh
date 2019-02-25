package com.gentics.mesh.storage.s3;

import static com.gentics.mesh.storage.s3.MinioContainer.MINIO_ACCESS_KEY;
import static com.gentics.mesh.storage.s3.MinioContainer.MINIO_SECRET_KEY;

import org.junit.Before;
import org.junit.ClassRule;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

public abstract class AbstractMinioTest {

	public static final String BUCKET_NAME = "mesh-test";

	public static final AwsCredentials credentials = AwsBasicCredentials.create(MINIO_ACCESS_KEY, MINIO_SECRET_KEY);

	@ClassRule
	public static MinioContainer minio = new MinioContainer();

	private S3Client s3;

	@Before
	public void setup() {
		s3 = createS3Client();
		createBucket("test123");
	}

	public S3Client createS3Client() {
		return S3Client.builder()
			.region(Region.US_EAST_1)
			.endpointOverride(minio.getURI())
			.credentialsProvider(StaticCredentialsProvider.create(credentials))
			.build();
	}

	public void createBucket(String name) {
		CreateBucketRequest createBucketRequest = CreateBucketRequest
			.builder()
			.bucket(name)
			.createBucketConfiguration(CreateBucketConfiguration.builder()
				.build())
			.build();
		s3.createBucket(createBucketRequest);
	}

}
