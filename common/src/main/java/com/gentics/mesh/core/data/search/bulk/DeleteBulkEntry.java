package com.gentics.mesh.core.data.search.bulk;

import com.gentics.mesh.search.SearchProvider;

import io.vertx.core.json.JsonObject;

public class DeleteBulkEntry implements BulkEntry {

	private static final String BULK_ACTION = "delete";

	private String indexName;

	private String documentId;

	public DeleteBulkEntry() {
	}

	public String getIndexName() {
		return indexName;
	}

	public DeleteBulkEntry setIndexName(String indexName) {
		this.indexName = indexName;
		return this;
	}

	public String getDocumentId() {
		return documentId;
	}

	public DeleteBulkEntry setDocumentId(String documentId) {
		this.documentId = documentId;
		return this;
	}

	@Override
	public String toBulkString() {
		JsonObject metaData = new JsonObject();
		metaData.put(BULK_ACTION,
			new JsonObject().put("_index", getIndexName()).put("_type", SearchProvider.DEFAULT_TYPE).put("_id", getDocumentId()));
		return metaData.encode();
	}
}
