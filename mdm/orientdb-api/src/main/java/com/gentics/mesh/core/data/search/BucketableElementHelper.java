package com.gentics.mesh.core.data.search;

import java.util.concurrent.ThreadLocalRandom;

public final class BucketableElementHelper {

	public static String BUCKET_ID_KEY = "bucketId";

	/**
	 * Return the bucketId for the element.
	 * 
	 * @param e
	 * @return
	 */
	public static Integer getBucketId(BucketableElement e) {
		Long bucketId = e.property(BUCKET_ID_KEY);
		if (bucketId == null) {
			e.generateBucketId();
			return e.<Long>getProperty(BUCKET_ID_KEY).intValue();
		} else {
			return bucketId.intValue();
		}
	}

	/**
	 * Set the bucketId for the element.
	 * 
	 * @param e
	 * @param bucketId
	 */
	public static void setBucketId(BucketableElement e, Integer bucketId) {
		if (bucketId == null) {
			e.removeProperty(BUCKET_ID_KEY);
		} else {
			e.property(BUCKET_ID_KEY, Long.valueOf(bucketId));
		}
	}

	/**
	 * Generate a new random bucketId.
	 * 
	 * @param e
	 */
	public static void generateBucketId(BucketableElement e) {
		int bucketId = ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);
		e.setBucketId(bucketId);
	}

}
