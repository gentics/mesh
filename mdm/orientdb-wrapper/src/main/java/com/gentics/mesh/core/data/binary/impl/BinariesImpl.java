package com.gentics.mesh.core.data.binary.impl;
import static com.gentics.mesh.util.StreamUtil.toStream;

import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.binary.Binaries;
import com.gentics.mesh.core.data.binary.Binary;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.core.db.Transactional;
import com.gentics.mesh.core.rest.node.field.BinaryCheckStatus;
import com.gentics.mesh.graphdb.spi.GraphDatabase;
import com.gentics.mesh.util.UUIDUtil;

/**
 * This class manages the {@link HibBinary} instances that have been persisted.
 */
@Singleton
public class BinariesImpl implements Binaries {

	private final GraphDatabase database;

	@Inject
	public BinariesImpl(GraphDatabase database) {
		this.database = database;
	}

	@Override
	public Transactional<HibBinary> create(String uuid, String sha512sum, Long size, BinaryCheckStatus checkStatus) {
		return database.transactional(tx -> {
			// TODO de-graph this
			HibBinary binary = ((GraphDBTx) tx).getGraph().addFramedVertex(BinaryImpl.class);
			binary.setSHA512Sum(sha512sum);
			binary.setSize(size);
			binary.setUuid(uuid);
			binary.setCheckStatus(checkStatus);

			if (checkStatus == BinaryCheckStatus.POSTPONED) {
				binary.setCheckSecret(UUIDUtil.randomUUID());
			}

			return binary;
		});
	}

	@Override
	public Transactional<HibBinary> findByHash(String hash) {
		return database.transactional(tx -> database.getVerticesTraversal(BinaryImpl.class, Binary.SHA512SUM_KEY, hash).nextOrNull());
	}

	@Override
	public Transactional<Stream<? extends HibBinary>> findByCheckStatus(BinaryCheckStatus checkStatus) {
		return database.transactional(tx -> toStream(database.getVerticesTraversal(BinaryImpl.class, Binary.BINARY_CHECK_STATUS_KEY, checkStatus)));
	}

	@Override
	public Transactional<Stream<HibBinary>> findAll() {
		return database.transactional(tx -> toStream(database.getElementsForType(BinaryImpl.class)));
	}
}
