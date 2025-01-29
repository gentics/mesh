package com.gentics.mesh.core.data.s3binary;

import java.util.stream.Stream;

import com.gentics.mesh.core.db.Transactional;
import com.gentics.mesh.core.rest.node.field.BinaryCheckStatus;

/**
 * Aggregation vertex for vertices which represent the binary.
 */
public interface S3Binaries {

	/**
	 * Find the s3 binary with the given s3ObjectKey.
	 * 
	 * @param s3ObjectKey
	 * @return
	 */
	Transactional<S3HibBinary> findByS3ObjectKey(String s3ObjectKey);

	/**
	 * Create a new S3 binary with the given virus check status.
	 * 
	 * @param uuid
	 *            Uuid of the binary
	 * @param objectKey
	 *            aws object key
	 * @param fileName
	 *            fileName
	 * @return
	 */
	Transactional<S3HibBinary> create(String uuid, String objectKey, String fileName, BinaryCheckStatus checkStatus);

	/**
	 * Return a transactional stream of all S3 binaries.
	 * 
	 * @return
	 */
	Transactional<Stream<S3HibBinary>> findAll();

	/**
	 * Create a new S3 binary.
	 * 
	 * @param uuid
	 *            Uuid of the binary
	 * @param objectKey
	 *            aws object key
	 * @param fileName
	 *            fileName
	 * @return
	 */
	default Transactional<S3HibBinary> create(String uuid, String objectKey, String fileName) {
		return create(uuid, objectKey, fileName, BinaryCheckStatus.ACCEPTED);
	}

	/**
	 * Find a binary with given UUID.
	 * 
	 * @param uuid
	 * @return
	 */
	Transactional<S3HibBinary> findByUuid(String uuid);
}