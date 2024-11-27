package com.gentics.mesh.core.data.search.request;

import static com.gentics.mesh.util.RxUtil.NOOP;

import java.util.Arrays;
import java.util.List;

import com.gentics.mesh.etc.config.search.ComplianceMode;
import com.gentics.mesh.search.SearchProvider;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.functions.Action;
import io.vertx.core.json.JsonObject;

/**
 * Document create request wrapper which can be used with RxJava and bulked into a batch request.
 */
public class CreateDocumentRequest implements Bulkable {

	private final String index;
	private final String transformedIndex;
	private final String id;
	private final String bulkPreamble;
	private final CachedJsonObjectProxy doc;
	private final Action onComplete;

	public CreateDocumentRequest(String index, String transformedIndex, String id, JsonObject doc, ComplianceMode mode) {
		this(index, transformedIndex, id, doc, mode, NOOP);
	}

	public CreateDocumentRequest(String index, String transformedIndex, String id, JsonObject doc, ComplianceMode mode, Action onComplete) {
		this.index = index;
		this.transformedIndex = transformedIndex;
		this.id = id;
		this.doc = new CachedJsonObjectProxy(doc);
		this.onComplete = onComplete;

		JsonObject settings = new JsonObject()
			.put("_index", transformedIndex)
			.put("_id", id);

		switch (mode) {
		case ES_7:
		case ES_8:
			break;
		case ES_6:
			settings.put("_type", SearchProvider.DEFAULT_TYPE);
			break;
		default:
			throw new RuntimeException("Unknown compliance mode {" + mode + "}");
		}

		this.bulkPreamble = new JsonObject().put("index", settings).encode();
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

	/**
	 * Return the document id.
	 * 
	 * @return
	 */
	public String getId() {
		return id;
	}

	/**
	 * Return the document which should be stored.
	 * 
	 * @return
	 */
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
