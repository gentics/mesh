package com.gentics.mesh.core.rest.node.field;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gentics.mesh.core.rest.node.field.binary.BinaryMetadata;
import com.gentics.mesh.core.rest.node.field.image.FocalPoint;

/**
 * A binary field is a field which can store binary and image related meta data.
 */
public interface BinaryField extends Field {

	/**
	 * Uuid of the used binary data.
	 * 
	 * @return
	 */
	String getBinaryUuid();

	/**
	 * Set the Uuid of the used binary data.
	 * 
	 * @param uuid
	 * @return
	 */
	BinaryField setBinaryUuid(String uuid);

	/**
	 * Return the binary filesize.
	 * 
	 * @return Filesize in bytes
	 */
	long getFileSize();

	/**
	 * Set the binary filesize.
	 * 
	 * @param fileSize
	 *            Filesize in bytes
	 * @return Fluent API
	 */
	BinaryField setFileSize(long fileSize);

	/**
	 * Return the image height.
	 * 
	 * @return Image height
	 */
	Integer getHeight();

	/**
	 * Set the image height.
	 * 
	 * @param height
	 *            Image height
	 * @return Fluent API
	 */
	BinaryField setHeight(Integer height);

	/**
	 * Return the width of the image.
	 * 
	 * @return Image width
	 */
	Integer getWidth();

	/**
	 * Set the width of the image.
	 * 
	 * @param width
	 *            Image width
	 * @return Fluent API
	 */
	BinaryField setWidth(Integer width);

	/**
	 * Return the binary mimetype.
	 * 
	 * @return Binary mimetype
	 */
	String getMimeType();

	/**
	 * Set the binary mimetype.
	 * 
	 * @param mimeType
	 *            Binary mimetype
	 * @return Fluent API
	 */
	BinaryField setMimeType(String mimeType);

	/**
	 * Return the sha512 checksum.
	 * 
	 * @return Checksum
	 */
	String getSha512sum();

	/**
	 * Set the binary sha512 checksum.
	 * 
	 * @param sha512sum
	 *            Checksum
	 * @return Fluent API
	 */
	BinaryField setSha512sum(String sha512sum);

	/**
	 * Return the binary filename of the node (may be null when no binary value was set)
	 * 
	 * @return Filename
	 */
	String getFileName();

	/**
	 * Set the binary filename.
	 * 
	 * @param fileName
	 *            Filename
	 * @return Fluent API
	 */
	BinaryField setFileName(String fileName);

	/**
	 * Return the dominant color of the image.
	 * 
	 * @return
	 */
	String getDominantColor();

	/**
	 * Set the dominant color of the image.
	 * 
	 * @param dominantColor
	 * @return
	 */
	BinaryField setDominantColor(String dominantColor);

	/**
	 * Return the currently configured focal point.
	 * 
	 * @return
	 */
	FocalPoint getFocalPoint();

	/**
	 * Set the focal point.
	 * 
	 * @param point
	 * @return
	 */
	BinaryField setFocalPoint(FocalPoint point);

	/**
	 * Set the focal point.
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	default BinaryField setFocalPoint(float x, float y) {
		setFocalPoint(new FocalPoint(x, y));
		return this;
	}

	/**
	 * Check whether contains any values.
	 * 
	 * @return
	 */
	@JsonIgnore
	boolean hasValues();

	/**
	 * Return the binary metadata.
	 * 
	 * @return
	 */
	BinaryMetadata getMetadata();

	/**
	 * Set the binary metadata.
	 * 
	 * @param metaData
	 * @return
	 */
	BinaryField setMetadata(BinaryMetadata metaData);

	@Override
	default Object getValue() {
		return getBinaryUuid();
	}

	/**
	 * Returns the plain text that was extracted from the uploaded document.
	 *
	 * @return
	 */
	String getPlainText();

	/**
	 * Set the plain text from the binary document.
	 *
	 * @return Fluent API
	 */
	BinaryField setPlainText(String text);

	String getStorageId();

	BinaryField setStorageId(String id);

}
