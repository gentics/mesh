package com.gentics.mesh.example;

import static com.gentics.mesh.example.AbstractExamples.DATE_OLD;
import static com.gentics.mesh.example.ExampleUuids.PLUGIN_1_UUID;
import static com.gentics.mesh.example.ExampleUuids.PLUGIN_2_UUID;
import static com.gentics.mesh.example.ExampleUuids.PLUGIN_3_UUID;
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
import com.gentics.mesh.core.rest.plugin.PluginManifest;
import com.gentics.mesh.core.rest.plugin.PluginResponse;

public class AdminExamples {

	public MeshStatusResponse createMeshStatusResponse(MeshStatus status) {
		return new MeshStatusResponse().setStatus(status);
	}

	public ClusterStatusResponse createClusterStatusResponse() {
		return new ClusterStatusResponse();
	}

	public PluginResponse createHelloWorldPluginResponse() {
		return createPluginResponse("Hello World 1", "hello-world1", PLUGIN_1_UUID);
	}
	
	public PluginResponse createPluginResponse(String name, String key, String uuid) {
		PluginManifest manifest = new PluginManifest();
		manifest.setName(name);
		manifest.setApiName(key);
		manifest.setAuthor("Joe Doe");
		manifest.setDescription("A dummy plugin");
		manifest.setInception(DATE_OLD);
		manifest.setLicense("Apache License 2.0");
		manifest.setVersion("1.0");
		manifest.validate();
		return new PluginResponse().setUuid(uuid).setName(manifest.getName()).setManifest(manifest);
	}

	public PluginListResponse createPluginListResponse() {
		PluginListResponse list = new PluginListResponse();
		list.add(createPluginResponse("Hello World 1", "hello-world1", PLUGIN_1_UUID));
		list.add(createPluginResponse("Hello World 2", "hello-world2", PLUGIN_2_UUID));
		list.add(createPluginResponse("Hello World 3", "hello-world3", PLUGIN_3_UUID));
		list.getMetainfo().setTotalCount(3);
		list.getMetainfo().setPageCount(1);
		list.getMetainfo().setCurrentPage(1);
		list.getMetainfo().setPerPage(25L);
		return list;
	}

	public PluginDeploymentRequest createPluginDeploymentRequest() {
		PluginDeploymentRequest request = new PluginDeploymentRequest();
		request.setName("filesystem:my-plugin.jar");
		return request;
	}

	public ConsistencyCheckResponse createConsistencyCheckResponse(boolean repaired) {
		ConsistencyCheckResponse response = new ConsistencyCheckResponse();
		response.getInconsistencies().add(new InconsistencyInfo().setSeverity(InconsistencySeverity.LOW).setElementUuid(UUID_1).setDescription(
			"A dangling field container has been found.").setRepairAction(RepairAction.DELETE).setRepaired(repaired));
		return response;
	}

}
