package com.gentics.mesh.search.verticle;

import com.gentics.elasticsearch.client.ElasticsearchClient;
import io.reactivex.Completable;
import io.vertx.core.json.JsonObject;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class BulkRequest implements Bulkable {

	private final String actions;

	public BulkRequest(List<Bulkable> requests) {
		actions = requests.stream()
			.flatMap(bulkable -> bulkable.toBulkActions().stream())
			.collect(Collectors.joining("\n"));
	}

	@Override
	public Completable execute(ElasticsearchClient<JsonObject> client) {
		return client.processBulk(actions).async().toCompletable();
	}

	@Override
	public List<String> toBulkActions() {
		return Collections.singletonList(actions);
	}
}
