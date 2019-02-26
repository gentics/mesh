package com.gentics.mesh.core.data.search.request;

import com.gentics.mesh.search.SearchProvider;
import io.reactivex.Completable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class BulkRequest implements Bulkable {
	private static final Logger log = LoggerFactory.getLogger(BulkRequest.class);

	private List<Bulkable> requests;

	public BulkRequest(List<Bulkable> requests) {
		this.requests = requests;
	}

	public BulkRequest(Bulkable... requests) {
		this(Arrays.asList(requests));
	}

	@Override
	public Completable execute(SearchProvider searchProvider) {
		return searchProvider.processBulk(requests);
	}

	@Override
	public List<String> toBulkActions() {
		return requests.stream()
			.flatMap(req -> req.toBulkActions().stream())
			.collect(Collectors.toList());
	}

	@Override
	public String toString() {
		return String.join("\n", toBulkActions());
	}

	public Collection<Bulkable> getRequests() {
		return requests;
	}
}
