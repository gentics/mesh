package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.rest.node.field.BinaryField;

/**
 * The BinaryField Domain Model interface.
 */
public interface BinaryGraphField extends BasicGraphField<BinaryField>, MeshVertex {
	/**
	 * Return the binary filename.
	 * 
	 * @return
	 */
	String getFileName();

	/**
	 * Copy the values of this field to the specified target field.
	 * 
	 * @param target
	 * @return Fluent API
	 */
	BinaryGraphField copyTo(BinaryGraphField target);

	/**
	 * Check whether the binary data represents an image.
	 * 
	 * @return
	 */
	boolean hasImage();

	/**
	 * Set the binary filename.
	 * 
	 * @param filenName
	 * @return Fluent API
	 */
	BinaryGraphField setFileName(String filenName);

	/**
	 * Return the binary mime type of the node.
	 * 
	 * @return
	 */
	String getMimeType();

	/**
	 * Set the binary mime type of the node.
	 * 
	 * @param mimeType
	 * @return Fluent API
	 */
	BinaryGraphField setMimeType(String mimeType);

	/**
	 * Set the binary file size in bytes
	 * 
	 * @param sizeInBytes
	 * @return Fluent API
	 */
	BinaryGraphField setFileSize(long sizeInBytes);

	/**
	 * Return the binary file size in bytes
	 * 
	 * @return
	 */
	long getFileSize();

	/**
	 * Set the binary SHA 512 checksum.
	 * 
	 * @param sha512HashSum
	 * @return Fluent API
	 */
	BinaryGraphField setSHA512Sum(String sha512HashSum);

	/**
	 * Return the binary SHA 512 checksum.
	 * 
	 * @return
	 */
	String getSHA512Sum();

	/**
	 * Set the binary image dominant color.
	 * 
	 * @param dominantColor
	 * @return Fluent API
	 */
	BinaryGraphField setImageDominantColor(String dominantColor);

	/**
	 * Return the binary image dominant color.
	 * 
	 * @return
	 */
	String getImageDominantColor();

	/**
	 * Return the binary image height.
	 * 
	 * @return
	 */
	Integer getImageHeight();

	/**
	 * Set the image width of the binary image.
	 * 
	 * @param width
	 * @return Fluent API
	 */
	BinaryGraphField setImageWidth(Integer width);

	/**
	 * Return the width of the binary image.
	 * 
	 * @return
	 */
	Integer getImageWidth();

	/**
	 * Set the with of the binary image. You can set this null to indicate that the binary data has no height.
	 * 
	 * @param heigth
	 * @return Fluent API
	 */
	BinaryGraphField setImageHeight(Integer heigth);

	/**
	 * Return the uuid of the binary field.
	 * 
	 * @return
	 */
	String getUuid();

	/**
	 * Set the uuid of the binary field.
	 * 
	 * @param uuid
	 */
	void setUuid(String uuid);

}
