package com.gentics.mesh.core.data.search.request;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.gentics.mesh.search.SearchProvider;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.functions.Action;

/**
 * Bulk request container for elasticsearch bulk operations.
 */
public class BulkRequest implements Bulkable {

	private final List<Bulkable> requests;
	private final Action onComplete;

	public BulkRequest(List<Bulkable> requests) {
		this.requests = requests;
		this.onComplete = () -> {
			for (SearchRequest request : requests) {
				request.onComplete().run();
			}
		};
	}

	public BulkRequest(Bulkable... requests) {
		this(Arrays.asList(requests));
	}

	@Override
	public int requestCount() {
		// TODO cache
		return requests.stream()
			.mapToInt(SearchRequest::requestCount)
			.sum();
	}

	@Override
	public Completable execute(SearchProvider searchProvider) {
		return searchProvider.processBulk(requests).doOnComplete(onComplete);
	}

	@Override
	public Single<List<String>> toBulkActions() {
		return Flowable.fromIterable(requests)
			.flatMapSingle(Bulkable::toBulkActions)
			.flatMapIterable(request -> request)
			.toList();
	}

	@Override
	public String toString() {
		return requests.size() + " bulked requests. " + requests;
	}

	public Collection<Bulkable> getRequests() {
		return requests;
	}

	@Override
	public long bulkLength() {
		// TODO cache
		return requests.stream()
			.mapToLong(Bulkable::bulkLength)
			.sum();
	}
}
