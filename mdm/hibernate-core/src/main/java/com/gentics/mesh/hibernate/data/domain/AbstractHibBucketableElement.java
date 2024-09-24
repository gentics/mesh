package com.gentics.mesh.hibernate.data.domain;

import jakarta.persistence.Embedded;
import jakarta.persistence.MappedSuperclass;

import com.gentics.mesh.core.data.HibBucketableElement;

/**
 * Common part of Hibernate entity, supported by search indexing with separating index buckets.
 * 
 * @author plyhun
 *
 */
@MappedSuperclass
public abstract class AbstractHibBucketableElement extends AbstractHibBaseElement implements HibBucketableElement {

	@Embedded
	protected BucketTracking bucketTracking = new BucketTracking();

	@Override
	public Integer getBucketId() {
		return bucketTracking.getBucketId();
	}

	@Override
	public void setBucketId(Integer bucketId) {
		bucketTracking.setBucketId(bucketId);
	}

	@Override
	public void generateBucketId() {
		bucketTracking.setBucketId(BucketTracking.generateBucketId());
	}	
}
