package com.gentics.mesh.core.data;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Domain model extension for elements which can be handled via buckets.
 */
public interface HibBucketableElement extends HibBaseElement {

	/**
	 * Return the bucketId of the element.
	 * 
	 * @return
	 */
	Integer getBucketId();

	/**
	 * Set the bucketId of the element.
	 * 
	 * @param bucketId
	 */
	void setBucketId(Integer bucketId);

	/**
	 * Generate a new random bucketId.
	 */
	default void generateBucketId() {
		int bucketId = ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);
		setBucketId(bucketId);
	}
}
