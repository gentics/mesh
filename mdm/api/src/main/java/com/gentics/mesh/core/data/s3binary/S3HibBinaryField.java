package com.gentics.mesh.core.data.s3binary;


import com.gentics.mesh.core.rest.node.field.s3binary.S3BinaryMetadata;

/**
 * MDM interface for the s3binary field information.
 */
public interface S3HibBinaryField {

	/**
	 * Return the referenced s3binary entity.
	 * 
	 * @return
	 */
	S3HibBinary getS3Binary();

	/**
	 * Return the binary metadata.
	 * 
	 * @return
	 */
	S3BinaryMetadata getMetadata();

	/**
	 * Return the filename of the s3binary in this field.
	 * 
	 * @return
	 */
	String getFileName();

	/**
	 * Return the mimetype of the s3binary.
	 * 
	 * @return
	 */
	String getMimeType();

}
