package com.gentics.mesh.core.data;

public interface HibBucketableElement {

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
	void generateBucketId();

}
