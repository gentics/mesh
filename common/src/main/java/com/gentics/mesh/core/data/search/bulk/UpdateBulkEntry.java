package com.gentics.mesh.core.data.search.bulk;

import com.gentics.mesh.search.SearchProvider;

import io.vertx.core.json.JsonObject;

/**
 * A bulk entry for document updates.
 */
public class UpdateBulkEntry extends AbstractBulkEntry {

	private final JsonObject payload;

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

	public JsonObject getPayload() {
		return payload;
	}

	@Override
	public Action getBulkAction() {
		return Action.UPDATE;
	}

	@Override
	public String toBulkString() {
		JsonObject metaData = new JsonObject();
		metaData.put(getBulkAction().id(),
			new JsonObject().put("_index", getIndexName()).put("_type", SearchProvider.DEFAULT_TYPE).put("_id", getDocumentId()));
		return new StringBuilder().append(metaData.encode()).append("\n").append(payload.encode()).toString();
	}

}
