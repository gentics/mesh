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

	public void setIndexSyncRunning(boolean indexSyncRunning) {
		this.indexSyncRunning = indexSyncRunning;
	}

	public Map<String, Object> getMetrics() {
		return metrics;
	}

	public void setMetrics(Map<String, Object> metrics) {
		this.metrics = metrics;
	}

}
