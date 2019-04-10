package com.gentics.mesh.core.data.search.request;

import com.gentics.mesh.search.SearchProvider;
import io.reactivex.Completable;
import io.reactivex.Single;
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
	private final Single<Boolean> usePipeline;

	public CreateDocumentRequest(String index, String transformedIndex, String id, JsonObject doc) {
		this(index, transformedIndex, id, doc, NOOP, Single.just(false));
	}

	public CreateDocumentRequest(String index, String transformedIndex, String id, JsonObject doc, Action onComplete) {
		this(index, transformedIndex, id, doc, onComplete, 	Single.just(false));
	}

	public CreateDocumentRequest(String index, String transformedIndex, String id, JsonObject doc, Action onComplete, Single<Boolean> usePipeline) {
		this.index = index;
		this.transformedIndex = transformedIndex;
		this.id = id;
		this.doc = doc;
		this.onComplete = onComplete;
		this.usePipeline = usePipeline;
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
	public Single<List<String>> toBulkActions() {
		return usePipeline.map(pipeline -> {
			JsonObject metadata = new JsonObject()
				.put("_index", transformedIndex)
				.put("_type", SearchProvider.DEFAULT_TYPE)
				.put("_id", id);
			if (pipeline) {
				metadata.put("pipeline", transformedIndex);
			}
			JsonObject jsonDoc = new JsonObject().put("index", metadata);
			return Arrays.asList(
				jsonDoc.encode(),
				doc.encode()
			);
		});
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
