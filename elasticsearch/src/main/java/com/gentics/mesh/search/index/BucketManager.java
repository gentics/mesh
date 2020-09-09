package com.gentics.mesh.search.index;

import com.gentics.mesh.core.data.MeshVertex;

public interface BucketManager {

	/**
	 * Compute the bucket size for the given amount of elements.
	 * 
	 * @param elementCount
	 * @return
	 */
	int getBucketSize(long elementCount);

	/**
	 * Store the computed bucketId in the given vertex.
	 * 
	 * @param vertex
	 */
	void store(MeshVertex vertex);

}
