package com.gentics.mesh.core.data.s3binary.impl;

import com.gentics.mesh.core.data.s3binary.S3Binaries;
import com.gentics.mesh.core.data.s3binary.S3Binary;
import com.gentics.mesh.core.data.s3binary.S3HibBinary;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.graphdb.spi.Transactional;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.stream.Stream;

import static com.gentics.mesh.util.StreamUtil.toStream;

/**
 * This class manages the {@link S3HibBinary} instances that have been persisted.
 */
@Singleton
public class S3BinariesImpl implements S3Binaries {

	private final Database database;

	@Inject
	public S3BinariesImpl(Database database) {
		this.database = database;
	}

	@Override
	public Transactional<S3HibBinary> create(String uuid, String sha512sum, Long size) {
		return database.transactional(tx -> {
			S3HibBinary binary = tx.getGraph().addFramedVertex(S3BinaryImpl.class);
			binary.setSHA512Sum(sha512sum);
			binary.setSize(size);
			binary.setUuid(uuid);
			return binary;
		});
	}

	@Override
	public Transactional<S3HibBinary> findByHash(String hash) {
		return database.transactional(tx -> database.getVerticesTraversal(S3BinaryImpl.class, S3Binary.SHA512SUM_KEY, hash).nextOrNull());
	}

	@Override
	public Transactional<Stream<S3HibBinary>> findAll() {
		return database.transactional(tx -> toStream(database.getVerticesForType(S3BinaryImpl.class)));
	}
}
