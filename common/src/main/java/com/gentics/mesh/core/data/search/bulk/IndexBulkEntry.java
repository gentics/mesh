package com.gentics.mesh.core.data.search.bulk;

import com.gentics.mesh.search.SearchProvider;

import io.vertx.core.json.JsonObject;

/**
 * Bulk entry for a document index operation.
 */
public class IndexBulkEntry extends AbstractBulkEntry {

	private final JsonObject payload;

	/**
	 * Flag which indicates whether the bulk entry should make use of a pipeline.
	 */
	private boolean usePipeline = false;

	/**
	 * Construct a new entry.
	 * 
	 * @param indexName
	 *            Name of the search index
	 * @param documentId
	 *            Id of the document
	 * @param payload
	 *            Document payload
	 */
	public IndexBulkEntry(String indexName, String documentId, JsonObject payload) {
		super(indexName, documentId);
		this.payload = payload;
	}

	public JsonObject getPayload() {
		return payload;
	}

	@Override
	public Action getBulkAction() {
		return Action.INDEX;
	}

	@Override
	public String toBulkString(String installationPrefix) {
		JsonObject metaData = new JsonObject();
		JsonObject settings = new JsonObject()
			.put("_index", installationPrefix + getIndexName())
			//.put("_type", SearchProvider.DEFAULT_TYPE)
			.put("_id", getDocumentId());

		if (usePipeline) {
			settings.put("pipeline", installationPrefix + getIndexName());
		}

		metaData.put(getBulkAction().id(), settings);
		return new StringBuilder().append(metaData.encode()).append("\n").append(payload.encode()).toString();
	}

}
