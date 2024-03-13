package com.gentics.mesh.core.data.s3binary;

import com.gentics.mesh.core.data.HibAntivirableBinaryElement;
import com.gentics.mesh.core.data.HibImageDataElement;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.s3binary.S3BinaryEventModel;

/**
 * Domain model for s3 binaries.
 */
public interface S3HibBinary extends HibImageDataElement, HibAntivirableBinaryElement {

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

	/**
	 * Create the specific delete event.
	 *
	 * @param uuid
	 * @param s3ObjectKey
	 * @return
	 */
	default S3BinaryEventModel onDeleted(String uuid, String s3ObjectKey) {
		S3BinaryEventModel event = new S3BinaryEventModel();
		event.setEvent(MeshEvent.S3BINARY_DELETED);
		event.setUuid(uuid);
		event.setS3ObjectKey(s3ObjectKey);
		return event;
	}

	/**
	 * Create the specific create event.
	 *
	 * @param uuid
	 * @param s3ObjectKey
	 * @return
	 */
	default S3BinaryEventModel onCreated(String uuid, String s3ObjectKey) {
		S3BinaryEventModel model = new S3BinaryEventModel();
		model.setEvent(MeshEvent.S3BINARY_CREATED);
		model.setUuid(uuid);
		model.setS3ObjectKey(s3ObjectKey);
		return model;
	}

	/**
	 * Create the specific metadata extraction event.
	 *
	 * @param uuid
	 * @param s3ObjectKey
	 * @return
	 */
	default S3BinaryEventModel onMetadataExtracted(String uuid, String s3ObjectKey) {
		S3BinaryEventModel model = new S3BinaryEventModel();
		model.setEvent(MeshEvent.S3BINARY_METADATA_EXTRACTED);
		model.setUuid(uuid);
		model.setS3ObjectKey(s3ObjectKey);
		return model;
	}

	@Override
	default Object getBinaryDataId() {
		return getS3ObjectKey();
	}
}
