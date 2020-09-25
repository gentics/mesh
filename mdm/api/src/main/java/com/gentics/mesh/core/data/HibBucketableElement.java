package com.gentics.mesh.core.data;

public interface HibBucketableElement {

	Integer getBucketId();

	void setBucketId(Integer bucketId);

	/**
	 * Generate a new random bucketId.
	 */
	void generateBucketId();

}
