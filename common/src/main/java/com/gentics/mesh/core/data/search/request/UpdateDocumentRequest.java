package com.gentics.mesh.core.data.search.request;

import com.gentics.mesh.search.SearchProvider;
import io.reactivex.Completable;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.List;

public class UpdateDocumentRequest implements Bulkable {
	private final String index;
	private final String transformedIndex;
	private final String id;
	private final JsonObject doc;

	public UpdateDocumentRequest(String index, String transformedIndex, String id, JsonObject doc) {
		this.index = index;
		this.transformedIndex = transformedIndex;
		this.id = id;
		this.doc = doc;
	}

	@Override
	public Completable execute(SearchProvider searchProvider) {
		return searchProvider.updateDocument(index, id, doc, true);
	}

	@Override
	public List<String> toBulkActions() {
		return Arrays.asList(
			new JsonObject()
				.put("update", new JsonObject()
					.put("_index", transformedIndex)
					.put("_type", SearchProvider.DEFAULT_TYPE)
					.put("_id", id)
				).encode(),
			new JsonObject()
				.put("doc", doc).encode()
		);
	}

	public String getIndex() {
		return index;
	}

	public String getTransformedIndex() {
		return transformedIndex;
	}

	public String getId() {
		return id;
	}

	public JsonObject getDoc() {
		return doc;
	}
}
