package com.gentics.mesh.core.data.search.bulk;

import com.gentics.mesh.search.SearchProvider;

import io.vertx.core.json.JsonObject;

public class UpdateBulkEntry implements BulkEntry {

	private static final String BULK_ACTION = "update";

	private String indexName;

	private String documentId;

	private JsonObject payload;

	public UpdateBulkEntry(String indexName, String documentId, JsonObject payload) {
		this.indexName = indexName;
		this.documentId = documentId;
		this.payload = payload;
	}

	public String getIndexName() {
		return indexName;
	}

	public String getDocumentId() {
		return documentId;
	}

	public JsonObject getPayload() {
		return payload;
	}

	@Override
	public String toBulkString() {
		JsonObject metaData = new JsonObject();
		metaData.put(BULK_ACTION,
			new JsonObject().put("_index", getIndexName()).put("_type", SearchProvider.DEFAULT_TYPE).put("_id", getDocumentId()));
		return new StringBuilder().append(metaData.encode()).append("\n").append(payload.encode()).toString();
	}

}
