package com.gentics.mesh.core.data.dao;


import java.io.InputStream;
import java.util.Base64;
import java.util.stream.Stream;

import com.gentics.mesh.core.data.HibBinaryDataElement;
import com.gentics.mesh.core.data.binary.Binaries;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.storage.BinaryStorage;
import com.gentics.mesh.core.db.Supplier;
import com.gentics.mesh.core.db.Transactional;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.node.field.BinaryCheckStatus;

import io.reactivex.Flowable;
import io.vertx.core.buffer.Buffer;

/**
 * Persistence-aware extension to {@link BinaryDao}
 *
 * @author plyhun
 *
 */
public interface PersistingBinaryDao extends BinaryDao {

	Base64.Encoder BASE64 = Base64.getEncoder();

	/**
	 * Get a binary storage implementation.
	 *
	 * @return
	 */
	Binaries binaries();

	@Override
	default Transactional<? extends HibBinary> findByHash(String hash) {
		return binaries().findByHash(hash);
	}

	@Override
	default Transactional<Stream<? extends HibBinary>> findByCheckStatus(BinaryCheckStatus checkStatus) {
		return binaries().findByCheckStatus(checkStatus);
	}

	@Override
	default Transactional<? extends HibBinary> create(String uuid, String hash, Long size, BinaryCheckStatus checkStatus) {
		return binaries().create(uuid, hash, size, checkStatus);
	}

	@Override
	default Transactional<Stream<HibBinary>> findAll() {
		return binaries().findAll();
	}

	@Override
	default Supplier<InputStream> openBlockingStream(HibBinary binary) {
		return binary.openBlockingStream();
	}

	@Override
	default Flowable<Buffer> getStream(HibBinaryDataElement binary) {
		BinaryStorage storage = Tx.get().data().binaryStorage();
		return storage.read(binary.getUuid());
	}

	@Override
	default String getBase64ContentSync(HibBinary binary) {
		Buffer buffer = Tx.get().data().binaryStorage().readAllSync(binary.getUuid());
		return BASE64.encodeToString(buffer.getBytes());
	}
}
