package com.gentics.mesh.core.data.binary;

import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.rest.node.field.image.Point;

/**
 * Domain model for binaries.
 */
public interface HibBinary extends HibBaseElement {

	/**
	 * Return the SHA512Sum of the binary.
	 * 
	 * @return
	 */
	String getSHA512Sum();

	/**
	 * Return the size of the binary data.
	 * 
	 * @return
	 */
	long getSize();

	/**
	 * Set the size of the binary data
	 * 
	 * @param sizeInBytes
	 * @return Fluent API
	 */
	HibBinary setSize(long sizeInBytes);

	/**
	 * Return the image height of the binary
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
	HibBinary setImageHeight(Integer height);

	/**
	 * Return the image width of the binary
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
	HibBinary setImageWidth(Integer width);

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
	HibBinary setSHA512Sum(String sha512sum);

	/**
	 * Set the binary uuid.
	 * 
	 * @param uuid
	 */
	void setUuid(String uuid);

}
