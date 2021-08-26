package com.gentics.mesh.rest;

import javax.inject.Inject;

import com.gentics.mesh.context.impl.LocalActionContextImpl;
import com.gentics.mesh.core.endpoint.admin.ClusterAdminHandler;
import com.gentics.mesh.core.rest.admin.cluster.ClusterConfigRequest;
import com.gentics.mesh.core.rest.admin.cluster.ClusterConfigResponse;
import com.gentics.mesh.core.rest.admin.cluster.coordinator.CoordinatorConfig;
import com.gentics.mesh.core.rest.admin.cluster.coordinator.CoordinatorMasterResponse;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.rest.client.MeshRequest;

public class MeshLocalClusterClientImpl extends MeshLocalClientImpl implements MeshLocalClusterClient {
	@Inject
	public ClusterAdminHandler adminHandler;

	@Inject
	public MeshLocalClusterClientImpl() {
		super();
	}

	@Override
	public MeshRequest<ClusterConfigResponse> loadClusterConfig() {
		LocalActionContextImpl<ClusterConfigResponse> ac = createContext(ClusterConfigResponse.class);
		adminHandler.handleLoadClusterConfig(ac);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<ClusterConfigResponse> updateClusterConfig(ClusterConfigRequest request) {
		LocalActionContextImpl<ClusterConfigResponse> ac = createContext(ClusterConfigResponse.class);
		adminHandler.handleUpdateClusterConfig(ac);
		return new MeshLocalRequestImpl<>(ac.getFuture());
	}

	@Override
	public MeshRequest<CoordinatorMasterResponse> loadCoordinationMaster() {
		return null;
	}

	@Override
	public MeshRequest<GenericMessageResponse> setCoordinationMaster() {
		return null;
	}

	@Override
	public MeshRequest<CoordinatorConfig> loadCoordinationConfig() {
		return null;
	}

	@Override
	public MeshRequest<CoordinatorConfig> updateCoordinationConfig(CoordinatorConfig coordinatorConfig) {
		return null;
	}
}
