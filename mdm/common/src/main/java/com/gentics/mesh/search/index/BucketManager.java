package com.gentics.mesh.search.index;

import com.gentics.mesh.core.data.Bucket;

import io.reactivex.Flowable;

/**
 * The bucketmanager generates a set of buckets for the given amount of total elements which are expected. A higher total count will result in more buckets
 * being created since the work for the ES sync will otherwise not be segmented into smaller chunks.
 */
public interface BucketManager {

	/**
	 * Return the buckets for the given amount of total elements.
	 * 
	 * @param totalCount
	 * @return
	 */
	Flowable<Bucket> getBuckets(long totalCount);

}
