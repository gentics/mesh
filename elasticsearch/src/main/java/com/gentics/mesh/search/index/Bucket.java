package com.gentics.mesh.search.index;

import static com.gentics.mesh.search.index.MappingHelper.BUCKET_ID_KEY;

import java.util.function.Predicate;

import com.gentics.mesh.search.BucketableElement;

import io.vertx.core.json.JsonObject;

public class Bucket {

	private long start;
	private long end;

	public Bucket(long start, long end) {
		this.start = start;
		this.end = end;
	}

	/**
	 * Start of bucket.
	 * 
	 * @return
	 */
	public long start() {
		return start;
	}

	/**
	 * End of bucket.
	 * 
	 * @return
	 */
	public long end() {
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

	/**
	 * Return the size of the bucket.
	 * @return
	 */
	public long size() {
		return end - start;
	}

	@Override
	public String toString() {
		return "Bucket: [" + start + "/" + end + "]";
	}

}
