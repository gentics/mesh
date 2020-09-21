package com.gentics.mesh.search.index;

import java.util.function.Predicate;

import com.gentics.mesh.search.BucketableElement;

import io.vertx.core.json.JsonObject;

public class Bucket {

	public static final String BUCKET_ID_KEY = "bucket_id";

	private int start;
	private int end;

	private int bucketNo;
	private int totalBuckets;

	public Bucket(int start, int end, int bucketNo, int totalBuckets) {
		this.start = start;
		this.end = end;
		this.bucketNo = bucketNo;
		this.totalBuckets = totalBuckets;
	}

	/**
	 * Start of bucket.
	 * 
	 * @return
	 */
	public int start() {
		return start;
	}

	/**
	 * End of bucket.
	 * 
	 * @return
	 */
	public int end() {
		return end;
	}

	/**
	 * Filter all elements which are within the bucket.
	 * 
	 * @return
	 */
	public Predicate<BucketableElement> filter() {
		return element -> {
			Integer bucketId = element.getBucketId();
			return isWithin(bucketId);
		};
	}

	/**
	 * Generate the Elasticsearch range query for this bucket.
	 * 
	 * @return
	 */
	public JsonObject rangeQuery() {
		JsonObject rangeQuery = new JsonObject();
		JsonObject rangeQueryParams = new JsonObject();
		rangeQueryParams.put("gte", start());
		rangeQueryParams.put("lte", end());
		rangeQuery.put(BUCKET_ID_KEY, rangeQueryParams);
		return new JsonObject().put("range", rangeQuery);
	}

	/**
	 * Test whether the given bucketId is within the bounds of the bucket
	 * 
	 * @param bucketId
	 * @return
	 */
	private boolean isWithin(Integer bucketId) {
		return bucketId <= end && bucketId >= start;
	}

	public int bucketNo() {
		return bucketNo;
	}

	public int total() {
		return totalBuckets;
	}

	@Override
	public String toString() {
		return "Bucket [" + bucketNo() + "/" + total() + "] for elements [" + start() + "/" + end() + "]";
	}

}
