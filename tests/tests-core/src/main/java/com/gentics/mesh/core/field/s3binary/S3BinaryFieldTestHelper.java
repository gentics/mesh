package com.gentics.mesh.core.field.s3binary;

import com.gentics.mesh.core.data.s3binary.S3HibBinary;
import com.gentics.mesh.core.data.s3binary.S3HibBinaryField;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.field.DataProvider;
import com.gentics.mesh.core.field.FieldFetcher;
import com.gentics.mesh.util.UUIDUtil;

public interface S3BinaryFieldTestHelper {

	final static String FILECONTENTS = "This is the file contents";

	final static String FILENAME = "test.txt";

	final static String MIMETYPE = "text/plain";

	final static FieldFetcher FETCH = (container, name) -> container.getS3Binary(name);

	final static DataProvider CREATE_EMPTY = (container, name) -> {
		// Empty binary fields can't be created since they need a connecting binary vertex
	};

	final DataProvider FILL_BASIC = (container, name) -> {
		S3HibBinary s3binary = Tx.get().s3binaries().create(UUIDUtil.randomUUID(), container.getUuid() + "/s3", FILENAME).runInExistingTx(Tx.get());
		S3HibBinaryField field = container.createS3Binary(name, s3binary);
		field.setFileName(FILENAME);
		field.setMimeType(MIMETYPE);
	};
}
