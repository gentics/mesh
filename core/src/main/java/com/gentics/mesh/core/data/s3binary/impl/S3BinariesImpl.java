package com.gentics.mesh.core.data.s3binary.impl;

import static com.gentics.mesh.util.StreamUtil.toStream;

import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.s3binary.S3Binaries;
import com.gentics.mesh.core.data.s3binary.S3Binary;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.graphdb.spi.Transactional;

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
	public Transactional<S3Binary> create(String uuid, String objectKey, String fileName) {
		return database.transactional(tx -> {
			S3Binary binary = tx.getGraph().addFramedVertex(S3BinaryImpl.class);
			binary.setS3ObjectKey(objectKey);
			binary.setUuid(uuid);
			binary.setFileName(fileName);
			return binary;
		});
	}

	@Override
	public Transactional<S3Binary> findByS3ObjectKey(String s3ObjectKey) {
		return database.transactional(tx -> database.getVerticesTraversal(S3BinaryImpl.class, S3Binary.S3_AWS_OBJECT_KEY, s3ObjectKey).nextOrNull());
	}

	@Override
	public Transactional<Stream<? extends S3Binary>> findAll() {
		return database.transactional(tx -> toStream(database.getVerticesForType(S3BinaryImpl.class)));
	}
}
