package com.gentics.mesh.core.rest.admin.cluster;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * REST model for a cluster configuration response.
 */
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
