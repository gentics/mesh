package com.gentics.mesh.core.data.search.index;

import io.vertx.core.json.JsonObject;

/**
 * Container for the index information.
 */
public class IndexInfo {
	private String indexName;
	private JsonObject indexSettings;
	private JsonObject indexMappings;
	private String sourceInfo;
	private JsonObject ingestPipelineSettings;

	/**
	 * Create a new index info which contains the information that is needed to create an index.
	 * 
	 * @param indexName
	 * @param indexSettings
	 * @param indexMappings
	 * @param sourceInfo
	 */
	public IndexInfo(String indexName, JsonObject indexSettings, JsonObject indexMappings, String sourceInfo) {
		this.indexName = indexName;
		this.indexSettings = indexSettings;
		this.indexMappings = indexMappings;
		this.sourceInfo = sourceInfo;
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

	public String getSourceInfo() {
		return sourceInfo;
	}

	@Override
	public String toString() {
		return "Info for index: " + indexName + " {" + sourceInfo + "}";
	}

}
