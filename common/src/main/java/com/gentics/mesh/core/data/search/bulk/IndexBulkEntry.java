package com.gentics.mesh.core.data.search.bulk;

import com.gentics.mesh.etc.config.search.ComplianceMode;
import com.gentics.mesh.search.SearchProvider;

import io.vertx.core.json.JsonObject;

/**
 * Bulk entry for a document index operation.
 */
public class IndexBulkEntry extends AbstractBulkEntry {

	private final JsonObject payload;

	/**
	 * Construct a new entry.
	 * 
	 * @param indexName
	 *            Name of the search index
	 * @param documentId
	 *            Id of the document
	 * @param payload
	 *            Document payload
	 * @param mode
	 *            Compliance mode that affects JSON format
	 */
	public IndexBulkEntry(String indexName, String documentId, JsonObject payload, ComplianceMode mode) {
		super(indexName, documentId, mode);
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
			.put("_id", getDocumentId());

		switch (getMode()) {
		case ES_7:
			break;
		case ES_6:
			settings.put("_type", SearchProvider.DEFAULT_TYPE);
			break;
		default:
			throw new RuntimeException("Unknown compliance mode {" + getMode() + "}");
		}

		metaData.put(getBulkAction().id(), settings);
		return new StringBuilder().append(metaData.encode()).append("\n").append(payload.encode()).toString();
	}

}
