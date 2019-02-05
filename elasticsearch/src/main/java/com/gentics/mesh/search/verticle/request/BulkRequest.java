package com.gentics.mesh.search.verticle.request;

import com.gentics.elasticsearch.client.ElasticsearchClient;
import io.reactivex.Completable;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

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

	@Override
	public Completable execute(ElasticsearchClient<JsonObject> client) {
		return client.processBulk(actions + "\n").async()
			.doOnSuccess(response -> {
				if (log.isTraceEnabled()) {
					log.trace("Response from Elasticsearch:\n" + response.encodePrettily());
				}
			})
			.toCompletable();
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
