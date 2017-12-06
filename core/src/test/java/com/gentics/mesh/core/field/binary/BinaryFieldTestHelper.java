package com.gentics.mesh.core.field.binary;

import com.gentics.mesh.core.data.binary.Binary;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.field.DataProvider;
import com.gentics.mesh.core.field.FieldFetcher;
import com.gentics.mesh.dagger.MeshInternal;

public interface BinaryFieldTestHelper {

	final static String FILECONTENTS = "This is the file contents";

	final static String FILENAME = "test.txt";

	final static String MIMETYPE = "text/plain";

	final static FieldFetcher FETCH = (container, name) -> container.getBinary(name);

	final static DataProvider CREATE_EMPTY = (container, name) -> {
		Binary binary = MeshInternal.get().boot().binaryRoot().create("bogus", 1L);
		container.createBinary(name, binary);
	};

	final DataProvider FILL_BASIC = (container, name) -> {
		Binary binary = MeshInternal.get().boot().binaryRoot().create("bogus", 1L);
		BinaryGraphField field = container.createBinary(name, binary);
		field.setFileName(FILENAME);
		field.setMimeType(MIMETYPE);
	};

}
