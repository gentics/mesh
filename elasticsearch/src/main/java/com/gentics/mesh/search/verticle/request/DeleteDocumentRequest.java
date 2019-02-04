package com.gentics.mesh.search.verticle.request;

import com.gentics.elasticsearch.client.ElasticsearchClient;
import com.gentics.mesh.search.SearchProvider;
import io.reactivex.Completable;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.List;

public class DeleteDocumentRequest implements Bulkable {
	private final String index;
	private final String id;

	public DeleteDocumentRequest(String index, String id) {
		this.index = index;
		this.id = id;
	}

	@Override
	public Completable execute(ElasticsearchClient<JsonObject> client) {
		return client.deleteDocument(index, SearchProvider.DEFAULT_TYPE, id).async().toCompletable();
	}

	@Override
	public List<String> toBulkActions() {
		return Arrays.asList(
			new JsonObject()
				.put("delete", new JsonObject()
					.put("_index", index)
					.put("_type", SearchProvider.DEFAULT_TYPE)
					.put("_id", id)
				).encode()
		);
	}
}
