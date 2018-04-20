package com.gentics.mesh.core.data.search.bulk;

import com.gentics.mesh.search.SearchProvider;

import io.vertx.core.json.JsonObject;

/**
 * Bulk entry for a delete operation.
 */
public class DeleteBulkEntry extends AbstractBulkEntry {

	/**
	 * Construct new entry.
	 *
	 * @param indexName
	 * @param documentId
	 */
	public DeleteBulkEntry(String indexName, String documentId) {
		super(indexName, documentId);
	}

	@Override
	public Action getBulkAction() {
		return Action.DELETE;
	}

	@Override
	public String toBulkString() {
		JsonObject metaData = new JsonObject();
		metaData.put(getBulkAction().id(),
			new JsonObject().put("_index", getIndexName()).put("_type", SearchProvider.DEFAULT_TYPE).put("_id", getDocumentId()));
		return metaData.encode();
	}
}
