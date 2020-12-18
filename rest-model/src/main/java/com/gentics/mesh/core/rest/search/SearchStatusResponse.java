package com.gentics.mesh.core.rest.search;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.annotation.Setter;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * Rest model POJO for a search status response.
 */
public class SearchStatusResponse implements RestModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Flag which indicates whether Elasticsearch is available and search queries can be executed.")
	boolean available = false;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Map which contains various metric values.")
	private Map<String, EntityMetrics> metrics = new HashMap<>();

	public SearchStatusResponse() {
	}

	public Map<String, EntityMetrics> getMetrics() {
		return metrics;
	}

	/**
	 * Set the metric information to the response.
	 * 
	 * @param metrics
	 * @return
	 */
	public SearchStatusResponse setMetrics(Map<String, EntityMetrics> metrics) {
		this.metrics = metrics;
		return this;
	}

	public boolean isAvailable() {
		return available;
	}

	@Setter
	public SearchStatusResponse setAvailable(boolean available) {
		this.available = available;
		return this;
	}
}
