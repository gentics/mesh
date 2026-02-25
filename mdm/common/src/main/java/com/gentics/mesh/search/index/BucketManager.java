package com.gentics.mesh.search.index;

import java.util.function.Supplier;

import org.reactivestreams.Publisher;

import com.gentics.mesh.core.data.Bucket;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;

/**
 * The bucketmanager generates a set of buckets for the given amount of total elements which are expected. A higher total count will result in more buckets
 * being created since the work for the ES sync will otherwise not be segmented into smaller chunks.
 */
public interface BucketManager {

	/**
	 * Return the buckets for the given amount of total elements.
	 * 
	 * @param totalCount
	 * @return
	 */
	Flowable<Bucket> getBuckets(long totalCount);

	/**
	 * Execute the handler for all buckets and return a flowable of all returned elements
	 * @param <T> type of the flowable objects
	 * @param totalCountSupplier supplier that will return the total count
	 * @param handler handler
	 * @return flowable
	 */
	<T> Flowable<T> doWithBuckets(Supplier<Long> totalCountSupplier, Function<Bucket, Publisher<? extends T>> handler);
}
