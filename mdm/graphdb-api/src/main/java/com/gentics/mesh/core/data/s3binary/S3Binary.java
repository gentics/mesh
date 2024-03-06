package com.gentics.mesh.core.data.s3binary;

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.node.field.S3BinaryGraphField;
import com.gentics.mesh.core.rest.node.field.BinaryCheckStatus;
import com.gentics.mesh.core.rest.node.field.image.Point;
import com.gentics.mesh.core.result.Result;

/**
 * Vertex which contains the actual information about the s3 binary content that is referenced by an {@link S3BinaryGraphField}.
 */
public interface S3Binary extends MeshVertex, S3HibBinary {

	String S3_BINARY_FILESIZE_PROPERTY_KEY = "s3binaryFileSize";

	String S3_BINARY_IMAGE_WIDTH_PROPERTY_KEY = "s3binaryImageWidth";

	String S3_BINARY_IMAGE_HEIGHT_PROPERTY_KEY = "s3binaryImageHeight";

	String S3_AWS_OBJECT_KEY = "s3ObjectKey";

	String S3_BINARY_CONTENT_TYPE = "s3binaryContentType";

	String S3_AWS_FILENAME = "filename";

	String BINARY_CHECK_STATUS_KEY = "checkStatus";

	String BINARY_CHECK_SECRET_KEY = "checkSecret";

	/**
	 * Return the s3binary size in bytes.
	 *
	 * @return
	 */
	default long getSize() {
		Long size = property(S3_BINARY_FILESIZE_PROPERTY_KEY);
		return size == null ? 0 : size;
	}

	/**
	 * Set the s3binary file size in bytes
	 *
	 * @param sizeInBytes
	 * @return Fluent API
	 */
	default S3HibBinary setSize(Long sizeInBytes) {
		property(S3_BINARY_FILESIZE_PROPERTY_KEY, sizeInBytes);
		return this;
	}

	/**
	 * Return the s3binary image height.
	 *
	 * @return
	 */
	default Integer getImageHeight() {
		return property(S3_BINARY_IMAGE_HEIGHT_PROPERTY_KEY);
	}

	/**
	 * Return the width of the s3binary image.
	 *
	 * @return
	 */
	default Integer getImageWidth() {
		return property(S3_BINARY_IMAGE_WIDTH_PROPERTY_KEY);
	}

	default String getS3ObjectKey() {	return property(S3_AWS_OBJECT_KEY);}

	default S3HibBinary setS3ObjectKey(String s3objectKey) {
		property(S3_AWS_OBJECT_KEY, s3objectKey);
		return this;
	}


	default String getFileName() {	return property(S3_AWS_FILENAME);}

	default S3Binary setFileName(String fileName) {
		property(S3_AWS_FILENAME, fileName);
		return this;
	}

	default String getMimeType() {	return property(S3_BINARY_CONTENT_TYPE);}

	default S3HibBinary setMimeType(String mimeType) {
		property(S3_BINARY_CONTENT_TYPE, mimeType);
		return this;
	}
	/**
	 * Set the with of the s3binary image. You can set this null to indicate that the s3binary data has no height.
	 *
	 * @param heigth
	 * @return Fluent API
	 */
	default S3Binary setImageHeight(Integer heigth) {
		property(S3_BINARY_IMAGE_HEIGHT_PROPERTY_KEY, heigth);
		return this;
	}

	/**
	 * Set the image width of the s3binary image.
	 *
	 * @param width
	 * @return Fluent API
	 */
	default S3HibBinary setImageWidth(Integer width) {
		property(S3_BINARY_IMAGE_WIDTH_PROPERTY_KEY, width);
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
	 * Find all s3binary fields which make use of this s3binary.
	 *
	 * @return
	 */
	Result<? extends S3BinaryGraphField> findFields();


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
	default S3Binary setCheckStatus(BinaryCheckStatus checkStatus) {
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
	default S3Binary setCheckSecret(String checkSecret) {
		property(BINARY_CHECK_SECRET_KEY, checkSecret);
		return this;
	}
}
