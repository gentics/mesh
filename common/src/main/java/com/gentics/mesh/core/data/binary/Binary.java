package com.gentics.mesh.core.data.binary;

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;

import io.vertx.core.buffer.Buffer;
import rx.Observable;

/**
 * Vertex which contains the actual information about the binary content that is referenced by an {@link BinaryGraphField}.
 */
public interface Binary extends MeshVertex {

	static final String SHA512SUM_KEY = "sha512sum";

	static final String BINARY_FILESIZE_PROPERTY_KEY = "binaryFileSize";

	static final String BINARY_IMAGE_WIDTH_PROPERTY_KEY = "binaryImageWidth";

	static final String BINARY_IMAGE_HEIGHT_PROPERTY_KEY = "binaryImageHeight";

	/**
	 * Return the binary data stream.
	 * 
	 * @return
	 */
	Observable<Buffer> getStream();

	/**
	 * Return the sha512 checksum.
	 * 
	 * @return
	 */
	default String getSHA512Sum() {
		return getProperty(SHA512SUM_KEY);
	}

	/**
	 * Set the SHA512 checksum.
	 * 
	 * @param sha512sum
	 * @return
	 */
	default Binary setSHA512Sum(String sha512sum) {
		setProperty(SHA512SUM_KEY, sha512sum);
		return this;
	}

	/**
	 * Return the binary size in bytes.
	 * 
	 * @return
	 */
	default long getSize() {
		Long size = getProperty(BINARY_FILESIZE_PROPERTY_KEY);
		return size == null ? 0 : size;
	}

	/**
	 * Set the binary file size in bytes
	 * 
	 * @param sizeInBytes
	 * @return Fluent API
	 */
	default Binary setSize(long sizeInBytes) {
		setProperty(BINARY_FILESIZE_PROPERTY_KEY, sizeInBytes);
		return this;
	}

	/**
	 * Return the binary image height.
	 * 
	 * @return
	 */
	default Integer getImageHeight() {
		return getProperty(BINARY_IMAGE_HEIGHT_PROPERTY_KEY);
	}

	/**
	 * Return the width of the binary image.
	 * 
	 * @return
	 */
	default Integer getImageWidth() {
		return getProperty(BINARY_IMAGE_WIDTH_PROPERTY_KEY);
	}

	/**
	 * Set the with of the binary image. You can set this null to indicate that the binary data has no height.
	 * 
	 * @param heigth
	 * @return Fluent API
	 */
	default Binary setImageHeight(Integer heigth) {
		setProperty(BINARY_IMAGE_HEIGHT_PROPERTY_KEY, heigth);
		return this;
	}

	/**
	 * Set the image width of the binary image.
	 * 
	 * @param width
	 * @return Fluent API
	 */
	default Binary setImageWidth(Integer width) {
		setProperty(BINARY_IMAGE_WIDTH_PROPERTY_KEY, width);
		return this;
	}

	/**
	 * Find all binary fields which make use of this binary.
	 * 
	 * @return
	 */
	Iterable<? extends BinaryGraphField> findFields();

}