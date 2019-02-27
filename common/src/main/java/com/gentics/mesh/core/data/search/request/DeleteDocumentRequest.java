package com.gentics.mesh.core.data.search.request;

import com.gentics.mesh.search.SearchProvider;
import io.reactivex.Completable;
import io.vertx.core.json.JsonObject;

import java.util.Collections;
import java.util.List;

public class DeleteDocumentRequest implements Bulkable {
	private final String index;
	private final String transformedIndex;
	private final String id;

	public DeleteDocumentRequest(String index, String transformedIndex, String id) {
		this.index = index;
		this.transformedIndex = transformedIndex;
		this.id = id;
	}

	@Override
	public int requestCount() {
		return 1;
	}

	@Override
	public Completable execute(SearchProvider searchProvider) {
		return searchProvider.deleteDocument(index, id);
	}

	@Override
	public List<String> toBulkActions() {
		return Collections.singletonList(
			new JsonObject()
				.put("delete", new JsonObject()
					.put("_index", transformedIndex)
					.put("_type", SearchProvider.DEFAULT_TYPE)
					.put("_id", id)
				).encode()
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
}
