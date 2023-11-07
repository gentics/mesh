package com.gentics.mesh.core.data.binary;

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.rest.node.field.BinaryCheckStatus;
import com.gentics.mesh.core.result.Result;

/**
 * Vertex which contains the actual information about the binary content that is referenced by an {@link BinaryGraphField}.
 */
public interface Binary extends MeshVertex, HibBinary {

	String SHA512SUM_KEY = "sha512sum";

	String BINARY_FILESIZE_PROPERTY_KEY = "binaryFileSize";

	String BINARY_IMAGE_WIDTH_PROPERTY_KEY = "binaryImageWidth";

	String BINARY_IMAGE_HEIGHT_PROPERTY_KEY = "binaryImageHeight";

	String BINARY_CHECK_STATUS_KEY = "checkStatus";

	String BINARY_CHECK_SECRET_KEY = "checkSecret";

	/**
	 * Find all binary fields which make use of this binary.
	 *
	 * @return
	 */
	Result<? extends BinaryGraphField> findFields();

	/**
	 * Return the sha512 checksum.
	 *
	 * @return
	 */
	default String getSHA512Sum() {
		return property(SHA512SUM_KEY);
	}

	/**
	 * Set the SHA512 checksum.
	 *
	 * @param sha512sum
	 * @return
	 */
	default HibBinary setSHA512Sum(String sha512sum) {
		property(SHA512SUM_KEY, sha512sum);
		return this;
	}

	/**
	 * Return the binary size in bytes.
	 *
	 * @return
	 */
	default long getSize() {
		Long size = property(BINARY_FILESIZE_PROPERTY_KEY);
		return size == null ? 0 : size;
	}

	/**
	 * Set the binary file size in bytes
	 *
	 * @param sizeInBytes
	 * @return Fluent API
	 */
	default HibBinary setSize(long sizeInBytes) {
		property(BINARY_FILESIZE_PROPERTY_KEY, sizeInBytes);
		return this;
	}

	/**
	 * Return the binary image height.
	 *
	 * @return
	 */
	default Integer getImageHeight() {
		return property(BINARY_IMAGE_HEIGHT_PROPERTY_KEY);
	}

	/**
	 * Return the width of the binary image.
	 *
	 * @return
	 */
	default Integer getImageWidth() {
		return property(BINARY_IMAGE_WIDTH_PROPERTY_KEY);
	}

	/**
	 * Set the with of the binary image. You can set this null to indicate that the binary data has no height.
	 *
	 * @param heigth
	 * @return Fluent API
	 */
	default HibBinary setImageHeight(Integer heigth) {
		property(BINARY_IMAGE_HEIGHT_PROPERTY_KEY, heigth);
		return this;
	}

	/**
	 * Set the image width of the binary image.
	 *
	 * @param width
	 * @return Fluent API
	 */
	default HibBinary setImageWidth(Integer width) {
		property(BINARY_IMAGE_WIDTH_PROPERTY_KEY, width);
		return this;
	}

	/**
	 * Return the check status of the binary (one of ACCEPTED, DENIED or POSTPONED).
	 * @return The check status of the binary.
	 */
	@Override
	default BinaryCheckStatus getCheckStatus() {
		Object status = property(BINARY_CHECK_STATUS_KEY);

		return status == null ? null : BinaryCheckStatus.valueOf(status.toString());
	}

	/**
	 * Set the check status of the binary (one of ACCEPTED, DENIED or POSTPONDED).
	 * @param checkStatus The check status to set.
	 * @return Fluent API.
	 */
	@Override
	default Binary setCheckStatus(BinaryCheckStatus checkStatus) {
		property(BINARY_CHECK_STATUS_KEY, checkStatus);
		return this;
	}

	/**
	 * Return the check secret of the binary.
	 * @return The check secret of the binary.
	 */
	@Override
	default String getCheckSecret() {
		return property(BINARY_CHECK_SECRET_KEY);
	}

	/**
	 * Set the check secret of the binary.
	 * @param checkSecret The binaries check secret.
	 * @return Fluent API.
	 */
	@Override
	default Binary setCheckSecret(String checkSecret) {
		property(BINARY_CHECK_SECRET_KEY, checkSecret);
		return this;
	}

	/**
	 * Get the manipulation variants of this binary image representation.
	 * 
	 * @return
	 */
	Result<? extends ImageVariant> getVariants();
}
