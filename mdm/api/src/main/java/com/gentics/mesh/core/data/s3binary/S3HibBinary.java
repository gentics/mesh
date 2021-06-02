package com.gentics.mesh.core.data.s3binary;

import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.rest.node.field.image.Point;

/**
 * Domain model for s3 binaries.
 */
public interface S3HibBinary extends HibBaseElement {

	/**
	 * Return the size of the s3binary data.
	 * 
	 * @return
	 */
	Long getSize();

	/**
	 * Set the size of the s3binary data
	 * 
	 * @param sizeInBytes
	 * @return Fluent API
	 */
	S3HibBinary setSize(Long sizeInBytes);

	/**
	 * Return the mime type of the s3binary data.
	 *
	 * @return
	 */
	String getMimeType();

	/**
	 * Set the mime type of the s3binary data
	 *
	 * @param mimeType
	 * @return S3HibBinary
	 */
	S3HibBinary setMimeType(String mimeType);

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
	 * Set the s3binary uuid.
	 * 
	 * @param uuid
	 */
	void setUuid(String uuid);

	/**
	 * Return the s3 object key
	 *
	 * @return s3 key or null when the information could not be determined
	 */
	String getS3ObjectKey();

	/**
	 * Set the S3 Object key
	 *
	 * @param s3ObjectKey
	 * @return
	 */
	S3HibBinary setS3ObjectKey(String s3ObjectKey);

	/**
	 * Return the filename
	 *
	 * @return Filename or null when the information could not be determined
	 */
	String getFileName();

	/**
	 * Set the S3 Binary Filename
	 *
	 * @param fileName
	 * @return
	 */
	S3HibBinary setFileName(String fileName);
}
