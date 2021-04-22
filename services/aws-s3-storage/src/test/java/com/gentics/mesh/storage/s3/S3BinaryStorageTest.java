package com.gentics.mesh.storage.s3;

import static org.junit.Assert.assertTrue;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.S3Options;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.Wait;

import com.gentics.mesh.core.data.binary.Binary;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;

import io.vertx.reactivex.core.Vertx;

@Ignore
public class S3BinaryStorageTest {

	public static final String VERSION = "RELEASE.2018-01-18T20-33-21Z";
	public static final String ACCESS_KEY = "myKey";
	public static final String SECRET_KEY = "mySecret";
	public static final String BUCKET_NAME = "mesh-test";

	private static Vertx vertx = Vertx.vertx();

	@ClassRule
	public static GenericContainer<?> minio = new GenericContainer<>("minio/minio:" + VERSION)
		.withCommand("server /data")
		.withEnv("MINIO_ACCESS_KEY", ACCESS_KEY)
		.withEnv("MINIO_SECRET_KEY", SECRET_KEY)
		.withExposedPorts(9000)
		.waitingFor(Wait.forHttp("/").forStatusCode(403));

	private S3BinaryStorageImpl storage;

	@Before
	public void setup() {
		MeshOptions meshOptions = new MeshOptions();
		S3Options options = new S3Options();
		options.setAccessKeyId(ACCESS_KEY);
		options.setSecretAccessKey(SECRET_KEY);
		options.setRegion("US_EAST_1");
		options.setBucket(BUCKET_NAME);
		meshOptions.setS3Options(options);
		//options.setUrl("http://localhost:" + minio.getMappedPort(9000));
		storage = new S3BinaryStorageImpl(meshOptions, vertx);
	}

	@Test
	public void testStore() {
		BinaryGraphField mockField = Mockito.mock(BinaryGraphField.class);
		HibBinary binary = Mockito.mock(Binary.class);
		Mockito.when(mockField.getBinary()).thenReturn(binary);
		Mockito.when(binary.getSHA512Sum()).thenReturn("test");
//		assertFalse(storage.exists(mockField));
//		storage.store(Flowable.just(Buffer.buffer("test")), "test").blockingAwait();
//		assertTrue(storage.exists(mockField));
		storage.read("test", "test").ignoreElements().blockingAwait();
	}

}
