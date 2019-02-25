package com.gentics.mesh.core.data.search.request;

import com.gentics.mesh.search.SearchProvider;
import io.reactivex.Completable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class BulkRequest implements Bulkable {
	private static final Logger log = LoggerFactory.getLogger(BulkRequest.class);

	private final String actions;

	public BulkRequest(Collection<Bulkable> requests) {
		actions = requests.stream()
			.flatMap(bulkable -> bulkable.toBulkActions().stream())
			.collect(Collectors.joining("\n"));
	}

	public BulkRequest(Bulkable... requests) {
		this(Arrays.asList(requests));
	}


	@Override
	public Completable execute(SearchProvider searchProvider) {
		return searchProvider.processBulk(actions + "\n");
	}

	@Override
	public List<String> toBulkActions() {
		return Collections.singletonList(actions);
	}

	@Override
	public String toString() {
		return actions;
	}
}
