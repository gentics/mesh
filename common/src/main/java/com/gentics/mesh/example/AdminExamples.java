package com.gentics.mesh.example;

import com.gentics.mesh.MeshStatus;
import com.gentics.mesh.core.rest.admin.cluster.ClusterStatusResponse;
import com.gentics.mesh.core.rest.admin.status.MeshStatusResponse;

public class AdminExamples {

	public MeshStatusResponse createMeshStatusResponse(MeshStatus status) {
		return new MeshStatusResponse().setStatus(status);
	}

	public ClusterStatusResponse createClusterStatusResponse() {
		return new ClusterStatusResponse();
	}

}
