package com.gentics.mesh.core.data.search.bulk;

import io.vertx.core.json.JsonObject;

/**
 * A bulk entry for document updates.
 */
public class UpdateBulkEntry extends AbstractBulkEntry {

	private final JsonObject payload;
	private boolean usePipeline = false;

	/**
	 * Construct a new entry.
	 * 
	 * @param indexName
	 * @param documentId
	 * @param payload
	 */
	public UpdateBulkEntry(String indexName, String documentId, JsonObject payload) {
		super(indexName, documentId);
		this.payload = payload;
	}

	/**
	 * Construct a new entry.
	 * 
	 * @param indexName
	 * @param documentId
	 * @param payload
	 * @param usePipeline
	 */
	public UpdateBulkEntry(String indexName, String documentId, JsonObject payload, boolean usePipeline) {
		this(indexName, documentId, payload);
		this.usePipeline = usePipeline;
	}

	public JsonObject getPayload() {
		return payload;
	}

	@Override
	public Action getBulkAction() {
		return Action.UPDATE;
	}

	@Override
	public String toBulkString(String installationPrefix) {
		JsonObject metaData = new JsonObject();
		JsonObject settings = new JsonObject()
			.put("_index", installationPrefix + getIndexName())
			//.put("_type", SearchProvider.DEFAULT_TYPE)
			.put("_id", getDocumentId());
		JsonObject doc = new JsonObject()
			.put("doc", payload);

		if (usePipeline) {
			settings.put("pipeline", installationPrefix + getIndexName());
		}
		metaData.put(getBulkAction().id(), settings);
		return new StringBuilder().append(metaData.encode()).append("\n").append(doc.encode()).toString();
	}

}
