package com.gentics.mesh.core.field.binary;

import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.binary.Binary;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.field.DataProvider;
import com.gentics.mesh.core.field.FieldFetcher;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.util.FileUtils;

import io.reactivex.Observable;
import io.vertx.core.buffer.Buffer;

public interface BinaryFieldTestHelper {

	String FILECONTENTS = "This is the file contents";

	String FILENAME = "test.txt";

	String MIMETYPE = "text/plain";

	FieldFetcher FETCH = GraphFieldContainer::getBinary;

	DataProvider CREATE_EMPTY = (container, name) -> {
		// Empty binary fields can't be created since they need a connecting binary vertex
	};

	DataProvider FILL_BASIC = (container, name) -> {
		Buffer buffer = Buffer.buffer(FILECONTENTS);
		String sha512Sum = FileUtils.hash(buffer).blockingGet();
		Binary binary = MeshInternal.get().boot().binaryRoot().create(sha512Sum, Long.valueOf(buffer.length()));
		MeshInternal.get().binaryStorage().store(Observable.just(buffer), binary.getUuid()).blockingAwait();
		BinaryGraphField field = container.createBinary(name, binary);
		field.setFileName(FILENAME);
		field.setMimeType(MIMETYPE);
	};

}
