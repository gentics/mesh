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
	 * @param hash
	 *            Hash sum of the binary
	 * @param size
	 *            Size in bytes
	 * @return
	 */
	Transactional<S3HibBinary> create(String uuid, String hash, Long size);

	/**
	 * Create a new binary.
	 * 
	 * @param hash
	 * @param size
	 * @return Transactional which executes the operation within a transaction
	 */
	default Transactional<S3HibBinary> create(String hash, long size) {
		return create(UUIDUtil.randomUUID(), hash, size);
	}

	/**
	 * Return a transactional stream of all binaries.
	 * 
	 * @return
	 */
	Transactional<Stream<S3HibBinary>> findAll();
}