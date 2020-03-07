package com.gentics.mesh.core.rest.admin.cluster.coordinator;

import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.etc.config.cluster.CoordinatorMode;

public class CoordinatorConfig implements RestModel {

	CoordinatorMode mode;

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
