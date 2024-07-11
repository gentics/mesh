package com.gentics.mesh.core.rest.node.field;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gentics.mesh.core.rest.node.field.binary.BinaryMetadataModel;
import com.gentics.mesh.core.rest.node.field.image.FocalPointModel;

/**
 * A binary field is a field which can store binary and image related meta data.
 */
public interface BinaryFieldModel extends FieldModel {

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
	BinaryFieldModel setBinaryUuid(String uuid);

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
	BinaryFieldModel setFileSize(long fileSize);

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
	BinaryFieldModel setHeight(Integer height);

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
	BinaryFieldModel setWidth(Integer width);

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
	BinaryFieldModel setMimeType(String mimeType);

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
	BinaryFieldModel setSha512sum(String sha512sum);

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
	BinaryFieldModel setFileName(String fileName);

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
	BinaryFieldModel setDominantColor(String dominantColor);

	/**
	 * Return the currently configured focal point.
	 *
	 * @return
	 */
	FocalPointModel getFocalPoint();

	/**
	 * Set the focal point.
	 *
	 * @param point
	 * @return
	 */
	BinaryFieldModel setFocalPoint(FocalPointModel point);

	/**
	 * Set the focal point.
	 *
	 * @param x
	 * @param y
	 * @return
	 */
	default BinaryFieldModel setFocalPoint(float x, float y) {
		setFocalPoint(new FocalPointModel(x, y));
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
	BinaryMetadataModel getMetadata();

	/**
	 * Set the binary metadata.
	 *
	 * @param metaData
	 * @return
	 */
	BinaryFieldModel setMetadata(BinaryMetadataModel metaData);

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
	 * @return
	 */
	BinaryFieldModel setPlainText(String text);

	/**
	 * Returns the binary check status.
	 *
	 * @return The binary check status.
	 */
	BinaryCheckStatus getCheckStatus();

	/**
	 * Set the binary check status.
	 * @param checkStatus The check status.
	 *
	 * @return Fluent API.
	 */
	BinaryFieldModel setCheckStatus(BinaryCheckStatus checkStatus);

	/**
	 * Returns the binary check secret.
	 *
	 * @return The binary check secret.
	 */
	String getCheckSecret();

	/**
	 * Set the binary check secret.
	 * @param checkSecret The binary check secret.
	 * @return Fluent API.
	 */
	BinaryFieldModel setCheckSecret(String checkSecret);
}
