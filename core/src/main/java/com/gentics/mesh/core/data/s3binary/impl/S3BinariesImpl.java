package com.gentics.mesh.core.data.s3binary.impl;

import com.gentics.mesh.core.data.s3binary.S3Binaries;
import com.gentics.mesh.core.data.s3binary.S3Binary;
import com.gentics.mesh.core.data.s3binary.S3HibBinary;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.core.db.Transactional;
import com.gentics.mesh.graphdb.spi.GraphDatabase;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.stream.Stream;

import static com.gentics.mesh.util.StreamUtil.toStream;

/**
 * This class manages the {@link S3HibBinary} instances that have been persisted.
 */
@Singleton
public class S3BinariesImpl implements S3Binaries {

	private final GraphDatabase database;

	@Inject
	public S3BinariesImpl(GraphDatabase database) {
		this.database = database;
	}

	@Override
	public Transactional<S3HibBinary> create(String uuid, String objectKey, String fileName) {
		return database.transactional(tx -> {
			S3HibBinary binary = ((GraphDBTx) tx).getGraph().addFramedVertex(S3BinaryImpl.class);
			binary.setS3ObjectKey(objectKey);
			binary.setUuid(uuid);
			binary.setFileName(fileName);
			return binary;
		});
	}

	@Override
	public Transactional<S3HibBinary> findByS3ObjectKey(String s3ObjectKey) {
		return database.transactional(tx -> database.getVerticesTraversal(S3BinaryImpl.class, S3Binary.S3_AWS_OBJECT_KEY, s3ObjectKey).nextOrNull());
	}

	@Override
	public Transactional<Stream<S3HibBinary>> findAll() {
		return database.transactional(tx -> toStream(database.getVerticesForType(S3BinaryImpl.class)));
	}
}
