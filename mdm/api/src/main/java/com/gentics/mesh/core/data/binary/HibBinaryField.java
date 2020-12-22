package com.gentics.mesh.core.data.binary;

import com.gentics.mesh.core.rest.node.field.binary.BinaryMetadata;

/**
 * MDM interface for the binary field information.
 */
public interface HibBinaryField {

	/**
	 * Return the referenced binary entity.
	 * 
	 * @return
	 */
	HibBinary getBinary();

	/**
	 * Return the binary metadata.
	 * 
	 * @return
	 */
	BinaryMetadata getMetadata();

	/**
	 * Return the filename of the binary in this field.
	 * 
	 * @return
	 */
	String getFileName();

	/**
	 * Return the mimetype of the binary.
	 * 
	 * @return
	 */
	String getMimeType();

}
