package com.gentics.mesh.search;

import com.gentics.mesh.core.data.MeshVertex;

public interface BucketableElement extends MeshVertex {

	String BUCKET_ID_KEY = "bucketId";

	/**
	 * Return the bucketId for the element.
	 * 
	 * @return
	 */
	default Integer getBucketId() {
		return property(BUCKET_ID_KEY);
	}

	/**
	 * Set the bucketId for the element.
	 * 
	 * @param bucketId
	 */
	default void setBucketId(Integer bucketId) {
		property(BUCKET_ID_KEY, bucketId);
	}
}
