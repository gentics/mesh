package com.gentics.mesh.core.rest.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * Rest model POJO for a search status response.
 */
public class SearchStatusResponse implements RestModel {

	@JsonProperty(required = false)
	@JsonPropertyDescription("Flag which indicates whether a reindex operation is currently running.")
	boolean reindexRunning = false;

	public SearchStatusResponse() {
	}

	public boolean isReindexRunning() {
		return reindexRunning;
	}

	public void setReindexRunning(boolean reindexRunning) {
		this.reindexRunning = reindexRunning;
	}
}
