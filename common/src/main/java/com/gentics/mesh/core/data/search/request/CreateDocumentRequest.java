package com.gentics.mesh.core.data.search.request;

import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.util.RxUtil;
import io.reactivex.Completable;
import io.reactivex.functions.Action;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.List;

import static com.gentics.mesh.util.RxUtil.NOOP;

public class CreateDocumentRequest implements Bulkable {
	private final String index;
	private final String transformedIndex;
	private final String id;
	private final JsonObject doc;
	private final Action onComplete;

	public CreateDocumentRequest(String index, String transformedIndex, String id, JsonObject doc) {
		this(index, transformedIndex, id, doc, NOOP);
	}

	public CreateDocumentRequest(String index, String transformedIndex, String id, JsonObject doc, Action onComplete) {
		this.index = index;
		this.transformedIndex = transformedIndex;
		this.id = id;
		this.doc = doc;
		this.onComplete = onComplete;
	}

	@Override
	public int requestCount() {
		return 1;
	}

	@Override
	public Completable execute(SearchProvider searchProvider) {
		return searchProvider.storeDocument(index, id, doc).doOnComplete(onComplete);
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

	@Override
	public Action onComplete() {
		return onComplete;
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
