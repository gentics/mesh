package com.gentics.mesh.core.data.dao;

import java.io.InputStream;
import java.util.stream.Stream;

import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.node.field.HibBinaryField;
import com.gentics.mesh.core.db.Supplier;
import com.gentics.mesh.core.db.Transactional;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.util.UUIDUtil;

import io.reactivex.Flowable;
import io.vertx.core.buffer.Buffer;

/**
 * DAO for {@link HibBinary}.
 */
public interface BinaryDao extends Dao<HibBinary> {

	/**
	 * Return the binary data stream.
	 *
	 * @return
	 */
	Flowable<Buffer> getStream(HibBinary binary);

	/**
	 * Return the data as base 64 encoded string in the same thread blockingly.
	 *
	 * @return
	 */
	String getBase64ContentSync(HibBinary binary);

	/**
	 * Find all binary fields which make use of this binary.
	 *
	 * @return
	 */
	Result<HibBinaryField> findFields(HibBinary binary);

	/**
	 * Opens a blocking {@link InputStream} to the binary file. This should only be used for some other blocking APIs (i.e. ImageIO)
	 *
	 * @return
	 */
	Supplier<InputStream> openBlockingStream(HibBinary binary);

	/**
	 * Find the binary with the given hashsum.
	 *
	 * @param hash
	 * @return
	 */
	Transactional<? extends HibBinary> findByHash(String hash);

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
	Transactional<? extends HibBinary> create(String uuid, String hash, Long size);

	/**
	 * Create a new binary.
	 * 
	 * @param hash
	 * @param size
	 * @return
	 */
	default Transactional<? extends HibBinary> create(String hash, long size) {
		return create(UUIDUtil.randomUUID(), hash, size);
	}

	/**
	 * Return a stream of binaries.
	 * 
	 * @return
	 */
	Transactional<Stream<HibBinary>> findAll();
}
