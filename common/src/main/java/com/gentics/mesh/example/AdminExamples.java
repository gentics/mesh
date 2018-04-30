package com.gentics.mesh.example;

import static com.gentics.mesh.util.UUIDUtil.randomUUID;

import com.gentics.mesh.MeshStatus;
import com.gentics.mesh.core.rest.admin.cluster.ClusterStatusResponse;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.core.rest.admin.consistency.InconsistencyInfo;
import com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity;
import com.gentics.mesh.core.rest.admin.consistency.RepairAction;
import com.gentics.mesh.core.rest.admin.status.MeshStatusResponse;
import com.gentics.mesh.core.rest.plugin.PluginDeploymentRequest;
import com.gentics.mesh.core.rest.plugin.PluginListResponse;
import com.gentics.mesh.core.rest.plugin.PluginManifest;
import com.gentics.mesh.core.rest.plugin.PluginResponse;
import com.gentics.mesh.util.DateUtils;
import com.gentics.mesh.util.UUIDUtil;

public class AdminExamples {

	public MeshStatusResponse createMeshStatusResponse(MeshStatus status) {
		return new MeshStatusResponse().setStatus(status);
	}

	public ClusterStatusResponse createClusterStatusResponse() {
		return new ClusterStatusResponse();
	}

	public PluginResponse createPluginResponse() {
		PluginManifest manifest = new PluginManifest();
		manifest.setName("Hello World");
		manifest.setApiName("hello-world");
		manifest.setAuthor("Joe Doe");
		manifest.setDescription("A dummy plugin");
		manifest.setInception(DateUtils.toISO8601(System.currentTimeMillis()));
		manifest.setLicense("Apache License 2.0");
		manifest.setVersion("1.0");
		manifest.validate();
		return new PluginResponse().setUuid(UUIDUtil.randomUUID()).setName(manifest.getName()).setManifest(manifest);
	}

	public PluginListResponse createPluginListResponse() {
		PluginListResponse list = new PluginListResponse();
		list.add(createPluginResponse());
		list.add(createPluginResponse());
		list.add(createPluginResponse());
		list.getMetainfo().setTotalCount(3);
		list.getMetainfo().setPageCount(1);
		list.getMetainfo().setCurrentPage(1);
		list.getMetainfo().setPerPage(25);
		return list;
	}

	public PluginDeploymentRequest createPluginDeploymentRequest() {
		PluginDeploymentRequest request = new PluginDeploymentRequest();
		request.setName("filesystem:my-plugin.jar");
		return request;
	}

	public ConsistencyCheckResponse createConsistencyCheckResponse(boolean repaired) {
		ConsistencyCheckResponse response = new ConsistencyCheckResponse();
		response.getInconsistencies().add(new InconsistencyInfo().setSeverity(InconsistencySeverity.LOW).setElementUuid(randomUUID()).setDescription(
			"A dangling field container has been found.").setRepairAction(RepairAction.DELETE).setRepaired(repaired));
		return response;
	}

}
