package com.gentics.mesh.search.index;

import com.gentics.mesh.search.BucketableElement;

import io.reactivex.Flowable;

public interface BucketManager {

	/**
	 * Return the buckets for the given class.
	 * 
	 * @param clazz
	 * @return
	 */
	Flowable<Bucket> getBuckets(Class<? extends BucketableElement> clazz);

}
