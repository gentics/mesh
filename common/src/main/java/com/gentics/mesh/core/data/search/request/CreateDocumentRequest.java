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
	private final String bulkPreamble;
	private final CachedJsonObjectProxy doc;
	private final Action onComplete;

	public CreateDocumentRequest(String index, String transformedIndex, String id, JsonObject doc) {
		this(index, transformedIndex, id, doc, NOOP);
	}

	public CreateDocumentRequest(String index, String transformedIndex, String id, JsonObject doc, Action onComplete) {
		this.index = index;
		this.transformedIndex = transformedIndex;
		this.id = id;
		this.doc = new CachedJsonObjectProxy(doc);
		this.onComplete = onComplete;
		this.bulkPreamble = new JsonObject().put("index", new JsonObject()
			.put("_index", transformedIndex)
			//.put("_type", SearchProvider.DEFAULT_TYPE)
			.put("_id", id)).encode();
	}

	@Override
	public int requestCount() {
		return 1;
	}

	@Override
	public Completable execute(SearchProvider searchProvider) {
		return searchProvider.storeDocument(index, id, doc.getProxyTarget()).doOnComplete(onComplete);
	}

	@Override
	public Single<List<String>> toBulkActions() {
		return Single.just(Arrays.asList(
			bulkPreamble,
			doc.encode()));
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
		return doc.getProxyTarget();
	}

	@Override
	public long bulkLength() {
		// +2 for newlines
		return bulkPreamble.length() + doc.encode().length() + 2;
	}

	@Override
	public String toString() {
		return "CreateDocumentRequest{" +
			"transformedIndex='" + transformedIndex + '\'' +
			", id='" + id + '\'' +
			'}';
	}
}
