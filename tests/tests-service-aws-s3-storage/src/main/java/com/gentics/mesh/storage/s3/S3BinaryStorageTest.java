package com.gentics.mesh.core.data.storage.s3;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.S3Options;
import com.gentics.mesh.core.data.storage.S3BinaryStorage;
import com.gentics.mesh.test.MeshOptionsTypeUnawareContext;
import com.gentics.mesh.test.docker.AWSContainer;

import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;

public class S3BinaryStorageTest implements MeshOptionsTypeUnawareContext {
    private static final String ACCESS_KEY = "accessKey";
    private static final String SECRET_KEY = "secretKey";
    private static final String BUCKET = "bucket";
    private static S3AsyncClient client = null;
    private S3BinaryStorage s3BinaryStorage;
    private static final Logger log = LoggerFactory.getLogger(S3BinaryStorageTest.class);
    private MeshOptions meshOptions;
    private AWSContainer container;

    @Before
    public void setup() {
        S3Options s3Options = new S3Options();
        s3Options.setBucket(BUCKET);
        s3Options.setSecretAccessKey(SECRET_KEY);
        s3Options.setAccessKeyId(ACCESS_KEY);
        meshOptions = getOptions();
        meshOptions.setS3Options(s3Options);
         container = new AWSContainer(
                new AWSContainer.CredentialsProvider(ACCESS_KEY, SECRET_KEY));
        container.start();
        S3AsyncClient client = getClient();
        createBucket(BUCKET);
        s3BinaryStorage = new S3BinaryStorageImpl(meshOptions, client);
    }

    @AfterClass
    public static void shutDown() {
        if (client != null) {
            DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder().bucket(BUCKET).build();
            client.deleteBucket(deleteBucketRequest);
        }
    }

    @Test
    public void textExists() {
            Boolean aBoolean = s3BinaryStorage.exists(BUCKET).blockingGet();
            assertNotNull(BUCKET);
            assertTrue(aBoolean);
    }

    @Test
    public void textNotExists() {
        Boolean aBoolean = s3BinaryStorage.exists("notexists").blockingGet();
        assertFalse(aBoolean);
    }

    private S3AsyncClient getClient() {
        client = getClient(container);
        return client;
    }

    private S3AsyncClient createBucket(String bucket) {
            CreateBucketRequest createRequest = CreateBucketRequest.builder()
                    .bucket(bucket)
                    .build();
            SingleInterop.fromFuture(this.client.createBucket(createRequest)).map(r -> r != null).blockingGet();
            return client;
    }

    @Test
    public void testCreateBucket() {
        String newBucketName = "new-bucket";
        s3BinaryStorage.createBucket(newBucketName).blockingGet();
        Boolean aBoolean = s3BinaryStorage.exists(newBucketName).blockingGet();
        assertTrue(aBoolean);
    }

    private S3AsyncClient getClient(AWSContainer container) {
        S3Configuration config = S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .checksumValidationEnabled(false)
                .build();
        S3AsyncClientBuilder clientBuilder = S3AsyncClient.builder();
            clientBuilder.endpointOverride(URI.create("http://" + container.getHostAddress()));
        AwsCredentials credentials = AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY);
        client = clientBuilder.region(Region.of("eu-central-1"))
                .serviceConfiguration(config)
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
        return client;
    }
}