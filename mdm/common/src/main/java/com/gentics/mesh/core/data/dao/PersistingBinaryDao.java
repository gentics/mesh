package com.gentics.mesh.core.data.dao;

import java.io.InputStream;
import java.util.stream.Stream;

import com.gentics.mesh.core.data.binary.Binaries;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.db.Supplier;
import com.gentics.mesh.core.db.Transactional;

/**
 * Persistence-aware extension to {@link BinaryDao}
 * 
 * @author plyhun
 *
 */
public interface PersistingBinaryDao extends BinaryDao {

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
	default Transactional<? extends HibBinary> create(String uuid, String hash, Long size) {
		return binaries().create(uuid, hash, size);
	}

	@Override
	default Transactional<Stream<HibBinary>> findAll() {
		return binaries().findAll();
	}

	@Override
	default Supplier<InputStream> openBlockingStream(HibBinary binary) {
		return binary.openBlockingStream();
	}
}
