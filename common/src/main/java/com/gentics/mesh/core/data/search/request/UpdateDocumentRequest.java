package com.gentics.mesh.core.data.search.request;

import com.gentics.mesh.search.SearchProvider;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.List;

public class UpdateDocumentRequest implements Bulkable {
	private final String index;
	private final String transformedIndex;
	private final String id;
	private final String bulkPreamble;
	private final CachedJsonObjectProxy doc;

	public UpdateDocumentRequest(String index, String transformedIndex, String id, JsonObject doc) {
		this.index = index;
		this.transformedIndex = transformedIndex;
		this.id = id;
		this.doc = new CachedJsonObjectProxy(doc);
		this.bulkPreamble = new JsonObject()
			.put("update", new JsonObject()
				.put("_index", transformedIndex)
				.put("_type", SearchProvider.DEFAULT_TYPE)
				.put("_id", id)
			).encode();
	}

	@Override
	public int requestCount() {
		return 1;
	}

	@Override
	public Completable execute(SearchProvider searchProvider) {
		return searchProvider.updateDocument(index, id, doc.getProxyTarget(), true);
	}

	@Override
	public Single<List<String>> toBulkActions() {
		return Single.just(Arrays.asList(
			bulkPreamble,
			new JsonObject()
				.put("doc", doc).encode()
		));
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
		// +10 for 2 newlines and {"doc":}
		return bulkPreamble.length() + doc.encode().length() + 10;
	}
}
