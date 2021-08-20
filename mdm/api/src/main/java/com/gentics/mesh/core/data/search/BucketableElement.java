package com.gentics.mesh.core.data.search;

import com.gentics.mesh.core.data.HibBaseElement;

/**
 * Bucketable elements are elements which will be indexed in elasticsearch. The bucket Id is used to partition the elasticsearch sync tasks. This reduces the
 * amount of memory that is needed when running the differential sync.
 *
 */
public interface BucketableElement extends HibBaseElement {

	/**
	 * Return the bucketId for the element.
	 * 
	 * @return
	 */
	Integer getBucketId();

	/**
	 * Set the bucketId for the element.
	 * 
	 * @param bucketId
	 */
	void setBucketId(Integer bucketId);

	/**
	 * Generate a new random bucketId.
	 */
	void generateBucketId();

}
