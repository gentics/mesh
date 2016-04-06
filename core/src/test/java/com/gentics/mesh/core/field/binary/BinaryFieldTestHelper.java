package com.gentics.mesh.core.field.binary;

import com.gentics.mesh.core.field.FieldFetcher;

public interface BinaryFieldTestHelper {

	final static String FILECONTENTS = "This is the file contents";

	final static String FILENAME = "test.txt";

	final static String MIMETYPE = "text/plain";

	static FieldFetcher FETCH = (container, name) -> container.getBinary(name);

}
