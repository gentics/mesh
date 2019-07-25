package com.gentics.mesh.example;

import static com.gentics.mesh.example.AbstractExamples.DATE_OLD;
import static com.gentics.mesh.example.ExampleUuids.UUID_1;

import com.gentics.mesh.MeshStatus;
import com.gentics.mesh.core.rest.admin.cluster.ClusterStatusResponse;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.core.rest.admin.consistency.InconsistencyInfo;
import com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity;
import com.gentics.mesh.core.rest.admin.consistency.RepairAction;
import com.gentics.mesh.core.rest.admin.status.MeshStatusResponse;
import com.gentics.mesh.core.rest.plugin.PluginDeploymentRequest;
import com.gentics.mesh.core.rest.plugin.PluginListResponse;
import com.gentics.mesh.core.rest.plugin.PluginResponse;
import com.gentics.mesh.plugin.PluginManifest;

public class AdminExamples {

	public MeshStatusResponse createMeshStatusResponse(MeshStatus status) {
		return new MeshStatusResponse().setStatus(status);
	}

	public ClusterStatusResponse createClusterStatusResponse() {
		return new ClusterStatusResponse();
	}

	public PluginResponse createHelloWorldPluginResponse() {
		return createPluginResponse("hello-world", "Hello World 1", "hello");
	}

	public PluginResponse createPluginResponse(String id, String name, String key) {
		PluginManifest manifest = new PluginManifest();
		manifest.setId(id);
		manifest.setName(name);
		manifest.setApiName(key);
		manifest.setAuthor("Joe Doe");
		manifest.setDescription("A dummy plugin");
		manifest.setInception(DATE_OLD);
		manifest.setLicense("Apache License 2.0");
		manifest.setVersion("1.0");
		return new PluginResponse().setId(id).setName(manifest.getName()).setManifest(manifest);
	}

	public PluginListResponse createPluginListResponse() {
		PluginListResponse list = new PluginListResponse();
		list.add(createPluginResponse("hello-world-1", "Hello World 1", "hello-world1"));
		list.add(createPluginResponse("hello-world-2", "Hello World 2", "hello-world2"));
		list.add(createPluginResponse("hello-world-3", "Hello World 3", "hello-world3"));
		list.getMetainfo().setTotalCount(3);
		list.getMetainfo().setPageCount(1);
		list.getMetainfo().setCurrentPage(1);
		list.getMetainfo().setPerPage(25L);
		return list;
	}

	public PluginDeploymentRequest createPluginDeploymentRequest() {
		PluginDeploymentRequest request = new PluginDeploymentRequest();
		request.setPath("my-plugin.jar");
		return request;
	}

	public ConsistencyCheckResponse createConsistencyCheckResponse(boolean repaired) {
		ConsistencyCheckResponse response = new ConsistencyCheckResponse();
		response.getInconsistencies().add(new InconsistencyInfo().setSeverity(InconsistencySeverity.LOW).setElementUuid(UUID_1).setDescription(
			"A dangling field container has been found.").setRepairAction(RepairAction.DELETE).setRepaired(repaired));
		return response;
	}

}
