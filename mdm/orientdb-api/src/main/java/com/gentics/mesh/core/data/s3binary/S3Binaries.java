package com.gentics.mesh.core.data.s3binary;

import com.gentics.mesh.graphdb.spi.Transactional;
import com.gentics.mesh.util.UUIDUtil;

import java.util.stream.Stream;

/**
 * Aggregation vertex for vertices which represent the binary.
 */
public interface S3Binaries {

	/**
	 * Find the binary with the given hashsum.
	 * 
	 * @param hash
	 * @return
	 */
	Transactional<S3HibBinary> findByHash(String hash);

	/**
	 * Create a new binary.
	 * 
	 * @param uuid
	 *            Uuid of the binary
	 * @param objectKey
	 *            aws object key
	 * @return
	 */
	Transactional<S3HibBinary> create(String uuid, String objectKey);

	/**
	 * Return a transactional stream of all binaries.
	 * 
	 * @return
	 */
	Transactional<Stream<S3HibBinary>> findAll();
}