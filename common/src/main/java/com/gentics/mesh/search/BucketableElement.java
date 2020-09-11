package com.gentics.mesh.search;

import java.util.concurrent.ThreadLocalRandom;

import com.gentics.mesh.core.data.MeshVertex;

/**
 * Bucketable elements are elements which will be indexed in elasticsearch. The bucket Id is used to partition the elasticsearch sync tasks. This reduces the
 * amount of memory that is needed when running the differential sync.
 *
 */
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

	/**
	 * Generate a new random bucketId.
	 */
	default void generateBucketId() {
		int bucketId = ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);
		setBucketId(bucketId);
	}

}
