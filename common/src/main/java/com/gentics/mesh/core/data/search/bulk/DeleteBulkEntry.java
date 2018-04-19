package com.gentics.mesh.core.data.search.bulk;

import com.gentics.mesh.search.SearchProvider;

import io.vertx.core.json.JsonObject;

/**
 * Bulk entry for a delete operation.
 */
public class DeleteBulkEntry implements BulkEntry {

	private static final String BULK_ACTION = "delete";

	private String indexName;

	private String documentId;

	public DeleteBulkEntry(String indexName, String documentId) {
		this.indexName = indexName;
		this.documentId = documentId;
	}

	public String getIndexName() {
		return indexName;
	}

	public String getDocumentId() {
		return documentId;
	}

	@Override
	public String toBulkString() {
		JsonObject metaData = new JsonObject();
		metaData.put(BULK_ACTION,
			new JsonObject().put("_index", getIndexName()).put("_type", SearchProvider.DEFAULT_TYPE).put("_id", getDocumentId()));
		return metaData.encode();
	}
}
