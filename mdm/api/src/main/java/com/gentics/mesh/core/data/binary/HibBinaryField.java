package com.gentics.mesh.core.data.binary;

import com.gentics.mesh.core.rest.node.field.binary.BinaryMetadata;

public interface HibBinaryField {

	HibBinary getBinary();

	BinaryMetadata getMetadata();

	String getFileName();

	String getMimeType();

}
