package com.gentics.mesh.core.data.search.request;

import static com.gentics.mesh.util.RxUtil.NOOP;

import java.util.Collections;
import java.util.List;

import com.gentics.mesh.core.data.search.Compliance;
import com.gentics.mesh.search.SearchProvider;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.functions.Action;
import io.vertx.core.json.JsonObject;

/**
 * Wrapper for bulkable delete document requests in elasticsearch.
 */
public class DeleteDocumentRequest implements Bulkable {

	private final String index;
	private final String transformedIndex;
	private final String id;
	private final Action onComplete;
	private final String bulkString;

	public DeleteDocumentRequest(String index, String transformedIndex, String id, Compliance compliance) {
		this(index, transformedIndex, id, compliance, NOOP);
	}

	public DeleteDocumentRequest(String index, String transformedIndex, String id, Compliance compliance, Action onComplete) {
		this.index = index;
		this.transformedIndex = transformedIndex;
		this.id = id;
		this.onComplete = onComplete;

		JsonObject settings = new JsonObject()
			.put("_index", transformedIndex)
			.put("_id", id);

		compliance.prepareDeleteRequest(settings);

		this.bulkString = new JsonObject().put("delete", settings).encode();
	}

	@Override
	public int requestCount() {
		return 1;
	}

	@Override
	public Completable execute(SearchProvider searchProvider) {
		return searchProvider.deleteDocument(index, id).doOnComplete(onComplete);
	}

	@Override
	public Single<List<String>> toBulkActions() {
		return Single.just(Collections.singletonList(bulkString));
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

	@Override
	public long bulkLength() {
		// + 1 for newline
		return bulkString.length() + 1;
	}

	@Override
	public String toString() {
		return "DeleteDocumentRequest{" +
			"transformedIndex='" + transformedIndex + '\'' +
			", id='" + id + '\'' +
			'}';
	}
}
