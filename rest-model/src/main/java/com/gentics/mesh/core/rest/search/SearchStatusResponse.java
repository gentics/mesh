package com.gentics.mesh.core.rest.search;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * Rest model POJO for a search status response.
 */
public class SearchStatusResponse implements RestModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Flag which indicates whether Elasticsearch is available and search queries can be executed.")
	boolean available = false;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Flag which indicates whether a index synchronization is currently running.")
	boolean indexSyncRunning = false;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Map which contains various metric values.")
	Map<String, Object> metrics = new HashMap<>();

	public SearchStatusResponse() {
	}

	public boolean isIndexSyncRunning() {
		return indexSyncRunning;
	}

	public SearchStatusResponse setIndexSyncRunning(boolean indexSyncRunning) {
		this.indexSyncRunning = indexSyncRunning;
		return this;
	}

	public Map<String, Object> getMetrics() {
		return metrics;
	}

	public SearchStatusResponse setMetrics(Map<String, Object> metrics) {
		this.metrics = metrics;
		return this;
	}

	public boolean isAvailable() {
		return available;
	}

	public SearchStatusResponse setAvailable(boolean available) {
		this.available = available;
		return this;
	}
}
