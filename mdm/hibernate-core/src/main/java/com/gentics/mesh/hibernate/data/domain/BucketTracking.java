package com.gentics.mesh.hibernate.data.domain;

import java.util.concurrent.ThreadLocalRandom;

import jakarta.persistence.Embeddable;

/**
 * Embeddable part of search index bucket. 
 * 
 * @see AbstractHibBucketableElement
 * @author plyhun
 *
 */
@Embeddable
public class BucketTracking {

	private Integer bucketId = generateBucketId();

	public Integer getBucketId() {
		return bucketId;
	}

	/**
	 * Set the bucketId for the element.
	 * 
	 * @param e
	 * @param bucketId
	 */
	public void setBucketId(Integer bucketId) {
		this.bucketId = bucketId; 
	}

	/**
	 * Generate a new random bucketId.
	 * 
	 * @param e
	 */
	public static Integer generateBucketId() {
		return ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);
	}
}
