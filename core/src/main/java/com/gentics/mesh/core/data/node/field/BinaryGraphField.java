package com.gentics.mesh.core.data.node.field;

import java.io.File;

import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.impl.BinaryFieldImpl;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import rx.Observable;

/**
 * The BinaryField Domain Model interface.
 */
public interface BinaryGraphField extends BasicGraphField<BinaryField> {

	FieldTransformator BINARY_TRANSFORMATOR = (container, ac, fieldKey, fieldSchema, languageTags, level, parentNode)-> {
		BinaryGraphField graphBinaryField = container.getBinary(fieldKey);
		if (graphBinaryField == null) {
			return Observable.just(new BinaryFieldImpl());
		} else {
			return graphBinaryField.transformToRest(ac);
		}
	};

	FieldUpdater  BINARY_UPDATER = (container, ac, fieldKey, restField, fieldSchema, schema) -> {
		BinaryGraphField graphBinaryField = container.getBinary(fieldKey);
		GraphField.failOnMissingMandatoryField(ac, graphBinaryField, restField, fieldSchema, fieldKey, schema);

		BinaryField binaryField = (BinaryFieldImpl) restField;
		if (restField == null) {
			return;
		}
		// Create new graph field if no existing one could be found
		if (graphBinaryField == null) {
			graphBinaryField = container.createBinary(fieldKey);
		}
		graphBinaryField.setImageDPI(binaryField.getDpi());
		graphBinaryField.setFileName(binaryField.getFileName());
		graphBinaryField.setMimeType(binaryField.getMimeType());
		// Don't update image width, height, SHA checksum - those are immutable
	};

	/**
	 * Return the binary filename.
	 * 
	 * @return
	 */
	String getFileName();

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
	 */
	void setFileName(String filenName);

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
	 */
	void setMimeType(String mimeType);

	/**
	 * Return future that holds a buffer reference to the binary file data.
	 * 
	 * @return
	 */
	Future<Buffer> getFileBuffer();

	/**
	 * Return the file that points to the binary file within the binary file storage.
	 * 
	 * @return Found file or null when no binary file could be found
	 */
	File getFile();

	/**
	 * Set the binary file size in bytes
	 * 
	 * @param sizeInBytes
	 */
	void setFileSize(long sizeInBytes);

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
	 */
	void setSHA512Sum(String sha512HashSum);

	/**
	 * Return the binary SHA 512 checksum.
	 * 
	 * @return
	 */
	String getSHA512Sum();

	/**
	 * Set the binary image DPI.
	 * 
	 * @param dpi
	 */
	void setImageDPI(Integer dpi);

	/**
	 * Return the binary image DPI.
	 * 
	 * @return
	 */
	Integer getImageDPI();

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
	 */
	void setImageWidth(Integer width);

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
	 */
	void setImageHeight(Integer heigth);

	/**
	 * Returns the segmented path that points to the binary file within the binary file location. The segmented path is build using the uuid of the binary field
	 * vertex.
	 * 
	 * @return
	 */
	String getSegmentedPath();

	/**
	 * Return the file path for the binary file location of the node.
	 * 
	 * @return
	 */
	String getFilePath();

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
