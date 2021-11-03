package com.gentics.mesh.rest.client.method;

import com.gentics.mesh.core.rest.admin.cluster.ClusterConfigRequest;
import com.gentics.mesh.core.rest.admin.cluster.ClusterConfigResponse;
import com.gentics.mesh.core.rest.admin.cluster.coordinator.CoordinatorConfig;
import com.gentics.mesh.core.rest.admin.cluster.coordinator.CoordinatorMasterResponse;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.rest.client.MeshRequest;

/**
 * Cluster management methods for an admin client.
 * 
 * @author plyhun
 *
 */
public interface ClusterAdminClientMethods extends AdminClientMethods {

	/**
	 * Return the cluster configuration.
	 * 
	 * @return
	 */
	MeshRequest<ClusterConfigResponse> loadClusterConfig();

	/**
	 * Update the cluster configuration.
	 * 
	 * @param request
	 *            New configuration
	 * @return Updated configuration
	 */
	MeshRequest<ClusterConfigResponse> updateClusterConfig(ClusterConfigRequest request);

	/**
	 * Returns information on the elected coordinator master.
	 * @return
	 */
	MeshRequest<CoordinatorMasterResponse> loadCoordinationMaster();

	/**
	 * Make this instance the coordination master.
	 * @return
	 */
	MeshRequest<GenericMessageResponse> setCoordinationMaster();

	/**
	 * Returns the currently active coordination configuration.
	 * @return
	 */
	MeshRequest<CoordinatorConfig> loadCoordinationConfig();

	/**
	 * Update the coordinator configuration of this instance. Note that the updated config will not be persisted.
	 * @param coordinatorConfig
	 * @return
	 */
	MeshRequest<CoordinatorConfig> updateCoordinationConfig(CoordinatorConfig coordinatorConfig);
}
