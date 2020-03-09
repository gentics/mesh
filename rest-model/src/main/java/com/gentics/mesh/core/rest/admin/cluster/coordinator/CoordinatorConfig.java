package com.gentics.mesh.core.rest.admin.cluster.coordinator;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.etc.config.cluster.CoordinatorMode;

/**
 * Configuration model for the coordination layer feature.
 */
public class CoordinatorConfig implements RestModel {

	@JsonProperty(required = false)
	@JsonPropertyDescription("Coordinator mode which can be DISABLED to disable coordination, CUD to handle read only requests or ALL to handle all requests.")
	private CoordinatorMode mode;

	public CoordinatorConfig() {
	}

	public CoordinatorMode getMode() {
		return mode;
	}

	public CoordinatorConfig setMode(CoordinatorMode mode) {
		this.mode = mode;
		return this;
	}

}
