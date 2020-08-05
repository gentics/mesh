package com.gentics.mesh.core.data.binary;

import java.util.stream.Stream;

import com.gentics.mesh.graphdb.spi.Transactional;
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
	Transactional<Binary> findByHash(String hash);

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
	Transactional<Binary> create(String uuid, String hash, Long size);

	default Transactional<Binary> create(String hash, long size) {
		return create(UUIDUtil.randomUUID(), hash, size);
	}

	Transactional<Stream<? extends Binary>> findAll();
}