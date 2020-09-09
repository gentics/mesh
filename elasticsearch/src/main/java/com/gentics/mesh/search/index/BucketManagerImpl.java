package com.gentics.mesh.search.index;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.etc.config.MeshOptions;

@Singleton
public class BucketManagerImpl implements BucketManager {

	private final MeshOptions options;

	@Inject
	public BucketManagerImpl(MeshOptions options) {
		this.options = options;
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

	public long randomBucketId() {
		double rnd = Math.random() * 10.0f;
		return (long) rnd;
	}

}
