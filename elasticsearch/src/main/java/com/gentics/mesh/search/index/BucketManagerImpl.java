package com.gentics.mesh.search.index;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;

@Singleton
public class BucketManagerImpl implements BucketManager {

	private final MeshOptions options;

	private final Database database;

	@Inject
	public BucketManagerImpl(MeshOptions options, Database database) {
		this.options = options;
		this.database = database;
	}

	@Override
	public int getBucketSize(long elementCount) {
		int batchSize = options.getSearchOptions().getSyncBatchSize();
		if (batchSize <= 0) {
			return 1;
		}
		long size = elementCount / batchSize;
		return (int) size;
	}

	public int randomBucketId(long count) {
		double rnd = Math.random() * getBucketSize(count);
		return (int) rnd;
	}

	@Override
	public void store(MeshVertex vertex) {
		long elementCount = database.count(vertex.getClass());
		int bucketId = randomBucketId(elementCount);
		System.out.println("Stored bucketId {" + bucketId + "} for type {" + vertex.getClass().getSimpleName() + "} with count {" + elementCount
			+ "}. Max Bucket is " + getBucketSize(elementCount));
		vertex.setBucketId(bucketId);
	}

}
