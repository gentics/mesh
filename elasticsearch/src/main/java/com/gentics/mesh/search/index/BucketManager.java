package com.gentics.mesh.search.index;

import com.gentics.mesh.search.BucketableElement;

import io.reactivex.Flowable;

public interface BucketManager {

	/**
	 * Compute the bucket size for the given amount of elements.
	 * 
	 * @param elementCount
	 * @return
	 */
	int getBucketPartitionCount(long elementCount);

	/**
	 * Store the computed bucketId in the given vertex.
	 * 
	 * @param element
	 */
	void store(BucketableElement element);

	/**
	 * Return the bucket partitions for the given class.
	 * 
	 * @param clazz
	 * @return
	 */
	Flowable<BucketPartition> getBucketPartitions(Class<? extends BucketableElement> clazz);

}
