package com.gentics.mesh.core.field.binary;

import com.gentics.mesh.core.data.binary.Binary;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.field.DataProvider;
import com.gentics.mesh.core.field.FieldFetcher;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.util.FileUtils;

import io.vertx.core.buffer.Buffer;
import rx.Observable;

public interface BinaryFieldTestHelper {

	final static String FILECONTENTS = "This is the file contents";

	final static String FILENAME = "test.txt";

	final static String MIMETYPE = "text/plain";

	final static FieldFetcher FETCH = (container, name) -> container.getBinary(name);

	final static DataProvider CREATE_EMPTY = (container, name) -> {
		// Empty binary fields can't be created since they need a connecting binary vertex
	};

	final DataProvider FILL_BASIC = (container, name) -> {
		Buffer buffer = Buffer.buffer(FILECONTENTS);
		String sha512Sum = FileUtils.hash(buffer).toBlocking().value();
		Binary binary = MeshInternal.get().boot().binaryRoot().create(sha512Sum, Long.valueOf(buffer.length()));
		MeshInternal.get().binaryStorage().store(Observable.just(buffer), binary.getUuid()).await();
		BinaryGraphField field = container.createBinary(name, binary);
		field.setFileName(FILENAME);
		field.setMimeType(MIMETYPE);
	};

}
