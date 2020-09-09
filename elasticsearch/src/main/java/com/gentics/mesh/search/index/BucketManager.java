package com.gentics.mesh.search.index;

public interface BucketManager {

	/**
	 * Compute the bucket size for the given amount of elements.
	 * 
	 * @param elementCount
	 * @return
	 */
	int getBucketSize(long elementCount);

}
