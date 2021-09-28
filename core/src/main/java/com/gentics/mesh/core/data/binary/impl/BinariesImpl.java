package com.gentics.mesh.core.data.binary.impl;

import static com.gentics.mesh.util.StreamUtil.toStream;

import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.binary.Binaries;
import com.gentics.mesh.core.data.binary.Binary;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.graphdb.spi.Transactional;

/**
 * This class manages the {@link HibBinary} instances that have been persisted.
 */
@Singleton
public class BinariesImpl implements Binaries {

	private final Database database;

	@Inject
	public BinariesImpl(Database database) {
		this.database = database;
	}

	@Override
	public Transactional<HibBinary> create(String uuid, String sha512sum, Long size) {
		return database.transactional(tx -> {
			// TODO de-graph this
			HibBinary binary = ((GraphDBTx) tx).getGraph().addFramedVertex(BinaryImpl.class);
			binary.setSHA512Sum(sha512sum);
			binary.setSize(size);
			binary.setUuid(uuid);
			return binary;
		});
	}

	@Override
	public Transactional<HibBinary> findByHash(String hash) {
		return database.transactional(tx -> database.getVerticesTraversal(BinaryImpl.class, Binary.SHA512SUM_KEY, hash).nextOrNull());
	}

	@Override
	public Transactional<Stream<HibBinary>> findAll() {
		return database.transactional(tx -> toStream(database.getVerticesForType(BinaryImpl.class)));
	}
}
