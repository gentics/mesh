package com.gentics.mesh.search.index;

import com.gentics.mesh.core.data.Bucket;

import io.reactivex.Flowable;

public interface BucketManager {

	/**
	 * Return the buckets for the given amount of total elements.
	 * 
	 * @param totalCount
	 * @return
	 */
	Flowable<Bucket> getBuckets(long totalCount);

}
