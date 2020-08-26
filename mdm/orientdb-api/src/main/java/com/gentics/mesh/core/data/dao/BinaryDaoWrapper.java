package com.gentics.mesh.core.data.dao;

import java.io.InputStream;
import java.util.stream.Stream;

import com.gentics.mesh.core.data.binary.Binary;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.graphdb.spi.Supplier;
import com.gentics.mesh.graphdb.spi.Transactional;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.util.UUIDUtil;

import io.reactivex.Flowable;
import io.vertx.core.buffer.Buffer;

public interface BinaryDaoWrapper extends BinaryDao {
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

	Transactional<Stream<Binary>> findAll();

	/**
	 * Return the binary data stream.
	 *
	 * @return
	 */
	Flowable<Buffer> getStream(Binary binary);

	/**
	 * Opens a blocking {@link InputStream} to the binary file. This should only be used for some other blocking APIs (i.e. ImageIO)
	 *
	 * @return
	 */
	Supplier<InputStream> openBlockingStream(Binary binary);

	/**
	 * Return the data as base 64 encoded string in the same thread blockingly.
	 *
	 * @return
	 */
	String getBase64ContentSync(Binary binary);

	/**
	 * Find all binary fields which make use of this binary.
	 *
	 * @return
	 */
	TraversalResult<BinaryGraphField> findFields(Binary binary);

}
