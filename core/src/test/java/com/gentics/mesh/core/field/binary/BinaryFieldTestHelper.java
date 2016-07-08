package com.gentics.mesh.core.field.binary;

import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.field.DataProvider;
import com.gentics.mesh.core.field.FieldFetcher;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.NoTrx;

public interface BinaryFieldTestHelper {

	final static String FILECONTENTS = "This is the file contents";

	final static String FILENAME = "test.txt";

	final static String MIMETYPE = "text/plain";

	final static FieldFetcher FETCH = (container, name) -> container.getBinary(name);

	final static DataProvider CREATE_EMPTY = (container, name) -> container.createBinary(name);

	final DataProvider FILL_BASIC = (container, name) -> {
		BinaryGraphField field = container.createBinary(name);
		field.setFileName(FILENAME);
		field.setMimeType(MIMETYPE);
		field.setSHA512Sum("bogus");
	};

}
