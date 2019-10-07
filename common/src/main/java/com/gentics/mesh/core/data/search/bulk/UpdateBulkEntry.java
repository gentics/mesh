package com.gentics.mesh.core.data.search.bulk;

import com.gentics.mesh.etc.config.search.ComplianceMode;
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
	 * @param mode
	 */
	public UpdateBulkEntry(String indexName, String documentId, JsonObject payload, ComplianceMode mode) {
		super(indexName, documentId, mode);
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
	public String toBulkString(String installationPrefix) {
		JsonObject metaData = new JsonObject();
		JsonObject settings = new JsonObject()
			.put("_index", installationPrefix + getIndexName())
			.put("_id", getDocumentId());

		switch (getMode()) {
		case PRE_ES_7:
			settings.put("_type", SearchProvider.DEFAULT_TYPE);
			break;
		default:
		}

		JsonObject doc = new JsonObject()
			.put("doc", payload);

		metaData.put(getBulkAction().id(), settings);
		return new StringBuilder().append(metaData.encode()).append("\n").append(doc.encode()).toString();
	}

}
