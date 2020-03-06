package com.gentics.mesh.core.rest.admin.cluster;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ClusterConfigResponse extends ClusterConfigRequest {

	/**
	 * Transform the response into a request.
	 * 
	 * @return
	 */
	@JsonIgnore
	public ClusterConfigRequest toRequest() {
		return this;
	}
}
