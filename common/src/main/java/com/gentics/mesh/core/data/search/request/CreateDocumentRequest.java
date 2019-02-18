package com.gentics.mesh.core.data.search.request;

import com.gentics.mesh.search.SearchProvider;
import io.reactivex.Completable;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.List;

public class CreateDocumentRequest implements Bulkable {
	private final String index;
	private final String id;
	private final JsonObject doc;

	public CreateDocumentRequest(String index, String id, JsonObject doc) {
		this.index = index;
		this.id = id;
		this.doc = doc;
	}

	@Override
	public Completable execute(SearchProvider searchProvider) {
		return searchProvider.storeDocument(index, id, doc);
	}

	@Override
	public List<String> toBulkActions() {
		return Arrays.asList(
			new JsonObject()
				.put("index", new JsonObject()
					.put("_index", index)
					.put("_type", SearchProvider.DEFAULT_TYPE)
					.put("_id", id)
				).encode(),
			doc.encode()
		);
	}
}
