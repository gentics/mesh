package com.gentics.mesh.core.data.s3binary;

import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.rest.node.field.image.Point;

/**
 * Domain model for binaries.
 */
public interface S3HibBinary extends HibBaseElement {

	/**
	 * Return the SHA512Sum of the s3binary.
	 * 
	 * @return
	 */
	String getSHA512Sum();

	/**
	 * Return the size of the s3binary data.
	 * 
	 * @return
	 */
	long getSize();

	/**
	 * Set the size of the s3binary data
	 * 
	 * @param sizeInBytes
	 * @return Fluent API
	 */
	S3HibBinary setSize(long sizeInBytes);

	/**
	 * Return the image height of the s3binary
	 * 
	 * @return Image height or null when the height could not be determined
	 */
	Integer getImageHeight();

	/**
	 * Set the image height.
	 * 
	 * @param height
	 * @return Fluent API
	 */
	S3HibBinary setImageHeight(Integer height);

	/**
	 * Return the image width of the s3binary
	 * 
	 * @return Image width or null when the width could not be determined
	 */
	Integer getImageWidth();

	/**
	 * Set the image width.
	 * 
	 * @param width
	 * @return Fluent API
	 */
	S3HibBinary setImageWidth(Integer width);

	/**
	 * Return the image size
	 * 
	 * @return Image size or null when the information could not be determined
	 */
	Point getImageSize();

	/**
	 * Set the SHA 512 Checksum
	 * 
	 * @param sha512sum
	 * @return
	 */
	S3HibBinary setSHA512Sum(String sha512sum);

	/**
	 * Set the s3binary uuid.
	 * 
	 * @param uuid
	 */
	void setUuid(String uuid);

}
