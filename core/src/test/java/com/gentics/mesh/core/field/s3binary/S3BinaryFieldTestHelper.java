package com.gentics.mesh.core.field.s3binary;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.core.data.node.field.S3BinaryGraphField;
import com.gentics.mesh.core.data.s3binary.S3Binary;
import com.gentics.mesh.core.field.DataProvider;
import com.gentics.mesh.core.field.FieldFetcher;
import com.gentics.mesh.core.graph.GraphAttribute;
import com.gentics.mesh.dagger.MeshComponent;

public interface S3BinaryFieldTestHelper {

	final static String FILECONTENTS = "This is the file contents";

	final static String FILENAME = "test.txt";

	final static String MIMETYPE = "text/plain";

	final static FieldFetcher FETCH = (container, name) -> container.getS3Binary(name);

	final static DataProvider CREATE_EMPTY = (container, name) -> {
		// Empty binary fields can't be created since they need a connecting binary vertex
	};

	final DataProvider FILL_BASIC = (container, name) -> {
		MeshComponent mesh = container.getGraphAttribute(GraphAttribute.MESH_COMPONENT);
		S3Binary s3binary = mesh.s3binaries().create(container.getUuid(), container.getUuid() + "/s3", FILENAME).runInExistingTx(Tx.get());

		S3BinaryGraphField field = container.createS3Binary(name, s3binary);
		field.setFileName(FILENAME);
		field.setMimeType(MIMETYPE);
	};
}
