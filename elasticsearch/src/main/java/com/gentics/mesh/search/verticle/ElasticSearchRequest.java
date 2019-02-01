package com.gentics.mesh.search.verticle;

import com.gentics.elasticsearch.client.ElasticsearchClient;
import io.reactivex.Completable;
import io.vertx.core.json.JsonObject;

public interface ElasticSearchRequest {
	Completable execute(ElasticsearchClient<JsonObject> client);
}
