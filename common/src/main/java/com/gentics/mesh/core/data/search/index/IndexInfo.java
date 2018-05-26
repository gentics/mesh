package com.gentics.mesh.core.data.search.index;

import io.vertx.core.json.JsonObject;

/**
 * Container for the index information.
 */
public class IndexInfo {
	private String indexName;
	private JsonObject indexSettings;
	private JsonObject indexMappings;

	private JsonObject ingestPipelineSettings;

	public IndexInfo(String indexName, JsonObject indexSettings, JsonObject indexMappings) {
		this.indexName = indexName;
		this.indexSettings = indexSettings;
		this.indexMappings = indexMappings;
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

	public String getIngestPipelineName() {
		return indexName;
	}

	public JsonObject getIngestPipelineSettings() {
		return ingestPipelineSettings;
	}

	public IndexInfo setIngestPipelineSettings(JsonObject ingestPipelineSettings) {
		this.ingestPipelineSettings = ingestPipelineSettings;
		return this;
	}

	@Override
	public String toString() {
		return "Info for index: " + indexName;
	}

}
