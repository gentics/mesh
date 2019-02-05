package com.gentics.mesh.search.verticle.request;

import com.gentics.elasticsearch.client.ElasticsearchClient;
import io.reactivex.Completable;
import io.vertx.core.json.JsonObject;

public interface ElasticsearchRequest {
	Completable execute(ElasticsearchClient<JsonObject> client);
}
