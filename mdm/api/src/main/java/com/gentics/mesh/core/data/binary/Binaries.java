package com.gentics.mesh.core.data.binary;

import java.util.stream.Stream;

import com.gentics.mesh.core.db.Transactional;
import com.gentics.mesh.core.rest.node.field.BinaryCheckStatus;
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
	 * Find the binaries with the specified check status.
	 *
	 * @param checkStatus The check status to filter for.
	 * @return A stream of matching binaries.
	 */
	Transactional<Stream<? extends Binary>> findByCheckStatus(BinaryCheckStatus checkStatus);

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
	Transactional<Binary> create(String uuid, String hash, Long size, BinaryCheckStatus checkStatus);

	default Transactional<Binary> create(String hash, long size) {
		return create(hash, size, BinaryCheckStatus.ACCEPTED);
	}

	/**
	 * Create a new binary.
	 *
	 * @param hash
	 * @param size
	 * @return Transactional which executes the operation within a transaction
	 */
	default Transactional<Binary> create(String hash, long size, BinaryCheckStatus checkStatus) {
		return create(UUIDUtil.randomUUID(), hash, size, checkStatus);
	}

	/**
	 * Return a transactional stream of all binaries.
	 *
	 * @return
	 */
	Transactional<Stream<Binary>> findAll();
}
