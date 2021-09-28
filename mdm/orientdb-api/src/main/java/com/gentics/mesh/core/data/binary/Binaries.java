package com.gentics.mesh.core.data.binary;

import java.util.stream.Stream;

import com.gentics.mesh.core.db.Transactional;
import com.gentics.mesh.util.UUIDUtil;

/**
 * Aggregation vertex for vertices which represent the binary.
 */
public interface Binaries {

	/**
	 * Find the binary with the given hashsum.
	 * 
	 * @param hash
	 * @return
	 */
	Transactional<HibBinary> findByHash(String hash);

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
	Transactional<HibBinary> create(String uuid, String hash, Long size);

	/**
	 * Create a new binary.
	 * 
	 * @param hash
	 * @param size
	 * @return Transactional which executes the operation within a transaction
	 */
	default Transactional<HibBinary> create(String hash, long size) {
		return create(UUIDUtil.randomUUID(), hash, size);
	}

	/**
	 * Return a transactional stream of all binaries.
	 * 
	 * @return
	 */
	Transactional<Stream<HibBinary>> findAll();
}