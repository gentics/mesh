package com.gentics.mesh.core.data.s3binary;

import com.gentics.mesh.graphdb.spi.Transactional;

import java.util.stream.Stream;

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
	Transactional<S3Binary> findByS3ObjectKey(String s3ObjectKey);

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
	Transactional<S3Binary> create(String uuid, String objectKey, String fileName);

	/**
	 * Return a transactional stream of all S3 binaries.
	 * 
	 * @return
	 */
	Transactional<Stream<S3Binary>> findAll();
}