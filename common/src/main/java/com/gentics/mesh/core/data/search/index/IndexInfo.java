package com.gentics.mesh.core.data.search.index;

import io.vertx.core.json.JsonObject;

/**
 * Container for the index information.
 */
public class IndexInfo {
	private String indexType;
	private String indexName;
	private JsonObject indexSettings;
	private JsonObject indexMappings;

	public IndexInfo(String indexName, String indexType, JsonObject indexSettings, JsonObject indexMappings) {
		this.indexType = indexType;
		this.indexName = indexName;
		this.indexSettings = indexSettings;
		this.indexMappings = indexMappings;
	}

	public String getIndexType() {
		return indexType;
	}

	public String getIndexName() {
		return indexName;
	}

	public JsonObject getIndexMappings() {
		return indexMappings;
	}

	public JsonObject getIndexSettings() {
		return indexSettings;
	}

	@Override
	public String toString() {
		return "Info for index: " + indexName + "/" + indexType;
	}

}
