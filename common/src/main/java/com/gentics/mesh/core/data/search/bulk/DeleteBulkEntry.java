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
	public String toBulkString(String installationPrefix) {
		JsonObject metaData = new JsonObject();
		JsonObject doc = new JsonObject()
			.put("_index", installationPrefix + getIndexName())
			//.put("_type", SearchProvider.DEFAULT_TYPE)
			.put("_id", getDocumentId());
		metaData.put(getBulkAction().id(), doc);
		return metaData.encode();
	}
}
