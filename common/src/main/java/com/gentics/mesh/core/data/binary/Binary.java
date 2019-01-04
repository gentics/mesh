package com.gentics.mesh.core.data.binary;

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.rest.node.field.image.Point;

import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.core.buffer.Buffer;

/**
 * Vertex which contains the actual information about the binary content that is referenced by an {@link BinaryGraphField}.
 */
public interface Binary extends MeshVertex {

	String SHA512SUM_KEY = "sha512sum";

	String BINARY_FILESIZE_PROPERTY_KEY = "binaryFileSize";

	String BINARY_IMAGE_WIDTH_PROPERTY_KEY = "binaryImageWidth";

	String BINARY_IMAGE_HEIGHT_PROPERTY_KEY = "binaryImageHeight";

	/**
	 * Return the binary data stream.
	 * 
	 * @return
	 */
	Flowable<Buffer> getStream();

	/**
	 * Return the base64 data stream.
	 * 
	 * @return
	 */
	Flowable<String> getBase64Stream();

	/**
	 * Return the data as base 64 encoded string.
	 * 
	 * @return
	 */
	Single<String> getBase64Content();

	/**
	 * Return the sha512 checksum.
	 * 
	 * @return
	 */
	default String getSHA512Sum() {
		return value(SHA512SUM_KEY);
	}

	/**
	 * Set the SHA512 checksum.
	 * 
	 * @param sha512sum
	 * @return
	 */
	default Binary setSHA512Sum(String sha512sum) {
		property(SHA512SUM_KEY, sha512sum);
		return this;
	}

	/**
	 * Return the binary size in bytes.
	 * 
	 * @return
	 */
	default long getSize() {
		Long size = value(BINARY_FILESIZE_PROPERTY_KEY);
		return size == null ? 0 : size;
	}

	/**
	 * Set the binary file size in bytes
	 * 
	 * @param sizeInBytes
	 * @return Fluent API
	 */
	default Binary setSize(long sizeInBytes) {
		property(BINARY_FILESIZE_PROPERTY_KEY, sizeInBytes);
		return this;
	}

	/**
	 * Return the binary image height.
	 * 
	 * @return
	 */
	default Integer getImageHeight() {
		return value(BINARY_IMAGE_HEIGHT_PROPERTY_KEY);
	}

	/**
	 * Return the width of the binary image.
	 * 
	 * @return
	 */
	default Integer getImageWidth() {
		return value(BINARY_IMAGE_WIDTH_PROPERTY_KEY);
	}

	/**
	 * Set the with of the binary image. You can set this null to indicate that the binary data has no height.
	 * 
	 * @param heigth
	 * @return Fluent API
	 */
	default Binary setImageHeight(Integer heigth) {
		property(BINARY_IMAGE_HEIGHT_PROPERTY_KEY, heigth);
		return this;
	}

	/**
	 * Set the image width of the binary image.
	 * 
	 * @param width
	 * @return Fluent API
	 */
	default Binary setImageWidth(Integer width) {
		property(BINARY_IMAGE_WIDTH_PROPERTY_KEY, width);
		return this;
	}

	/**
	 * Return the image size.
	 * 
	 * @return
	 */
	default Point getImageSize() {
		Integer x = getImageHeight();
		Integer y = getImageWidth();
		if (x == null || y == null) {
			return null;
		} else {
			return new Point(x, y);
		}
	}

	/**
	 * Find all binary fields which make use of this binary.
	 * 
	 * @return
	 */
	Iterable<? extends BinaryGraphField> findFields();

}