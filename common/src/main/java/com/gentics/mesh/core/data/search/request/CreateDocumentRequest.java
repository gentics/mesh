package com.gentics.mesh.core.data.search.request;

import com.gentics.mesh.search.SearchProvider;
import io.reactivex.Completable;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.List;

public class CreateDocumentRequest implements Bulkable {
	private final String index;
	private final String transformedIndex;
	private final String id;
	private final JsonObject doc;

	public CreateDocumentRequest(String index, String transformedIndex, String id, JsonObject doc) {
		this.index = index;
		this.transformedIndex = transformedIndex;
		this.id = id;
		this.doc = doc;
	}

	@Override
	public int requestCount() {
		return 1;
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
					.put("_index", transformedIndex)
					.put("_type", SearchProvider.DEFAULT_TYPE)
					.put("_id", id)
				).encode(),
			doc.encode()
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
