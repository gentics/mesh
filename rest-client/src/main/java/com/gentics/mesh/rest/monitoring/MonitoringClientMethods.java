package com.gentics.mesh.rest.monitoring;

import com.gentics.mesh.core.rest.MeshServerInfoModel;
import com.gentics.mesh.core.rest.admin.cluster.ClusterStatusResponse;
import com.gentics.mesh.core.rest.admin.status.MeshStatusResponse;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.impl.EmptyResponse;

public interface MonitoringClientMethods {

	/**
	 * Return the Gentics Mesh server status.
	 * 
	 * @return
	 */
	MeshRequest<MeshStatusResponse> status();

	/**
	 * Return the Gentics Mesh cluster status.
	 * 
	 * @return
	 */
	MeshRequest<ClusterStatusResponse> clusterStatus();

	/**
	 * Load the current metrics.
	 * 
	 * @return
	 */
	MeshRequest<String> metrics();

	MeshRequest<EmptyResponse> ready();

	MeshRequest<EmptyResponse> live();

	/**
	 * Load the mesh server API Info
	 * 
	 * @return
	 */
	MeshRequest<MeshServerInfoModel> versions();

}
