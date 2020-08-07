package com.gentics.mesh.storage.s3;

import static com.gentics.mesh.storage.s3.MinioContainer.MINIO_ACCESS_KEY;
import static com.gentics.mesh.storage.s3.MinioContainer.MINIO_SECRET_KEY;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.gentics.mesh.core.data.binary.Binary;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.util.RxUtil;
import com.gentics.mesh.util.UUIDUtil;

import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.core.buffer.Buffer;
import io.vertx.reactivex.core.Vertx;

public class S3BinaryStorageTest extends AbstractMinioTest {

	private static Vertx vertx = Vertx.vertx();

	private S3BinaryStorage storage;

	@Before
	public void setup() {
		S3StorageOptions options = new S3StorageOptions();
		options.setAccessId(MINIO_ACCESS_KEY);
		options.setAccessKey(MINIO_SECRET_KEY);
		options.setRegion("US_EAST_1");
		options.setBucketName(BUCKET_NAME);
		options.setUrl(minio.getURI().toString());
		storage = new S3BinaryStorage(options, vertx);
	}

	@Test
	public void testStore() throws IOException {
		Buffer data = Buffer.buffer("test1234");
		Flowable<Buffer> flow = Flowable.just(data);
		String uuid = UUIDUtil.randomUUID();
		String temporaryId = UUIDUtil.randomUUID();

		Binary binary = Mockito.mock(Binary.class);
		Mockito.when(binary.getSHA512Sum()).thenReturn(uuid);

		BinaryGraphField mockField = Mockito.mock(BinaryGraphField.class);
		Mockito.when(mockField.getBinary()).thenReturn(binary);

		// Check initialy - file does not exists
		assertFalse(storage.exists(mockField).blockingGet());

		// Upload in temp
		storage.storeInTemp(flow, data.length(), temporaryId).blockingAwait();
		assertFalse(storage.exists(mockField).blockingGet());

		// Move into place
		storage.moveInPlace(uuid, temporaryId).blockingAwait();

		// Check file exists
		assertTrue(storage.exists(mockField).blockingGet());

		// Read file
		Single<Buffer> buf = RxUtil.readEntireData(storage.read(uuid));
		System.out.println("Data: " + buf.blockingGet().toString());

		// Delete file & check
		storage.delete(uuid).blockingAwait();
		assertFalse(storage.exists(mockField).blockingGet());

		flow = Flowable.just(data);
		storage.storeInTemp(flow, data.length(), temporaryId).blockingAwait();
		storage.purgeTemporaryUpload(temporaryId);
	}

}
