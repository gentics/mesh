package com.gentics.mesh.distributed;

import static com.gentics.mesh.test.ClientHelper.call;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.test.docker.MeshContainer;

import io.vertx.core.Vertx;

public abstract class AbstractClusterTest {

	public static Vertx vertx = Vertx.vertx();

	protected NodeResponse createProjectAndNode(MeshRestClient client, String projectName) {

		// Node A: Create Project
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName(projectName);
		request.setSchemaRef("folder");
		ProjectResponse projectResponse = call(() -> client.createProject(request));
		String folderUuid = projectResponse.getRootNode().getUuid();

		// Node A: Find the content schema
		SchemaListResponse schemaListResponse = call(() -> client.findSchemas());
		String contentSchemaUuid = schemaListResponse.getData().stream().filter(sr -> sr.getName().equals("content")).map(sr -> sr.getUuid())
			.findAny().get();

		// Node A: Assign content schema to project
		call(() -> client.assignSchemaToProject(projectName, contentSchemaUuid));

		// Node A: Create node
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.getFields().put("teaser", FieldUtil.createStringField("some rorschach teaser"));
		nodeCreateRequest.getFields().put("slug", FieldUtil.createStringField("new-page.html"));
		nodeCreateRequest.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
		nodeCreateRequest.setSchemaName("content");
		nodeCreateRequest.setParentNodeUuid(folderUuid);

		NodeResponse response = call(() -> client.createNode(projectName, nodeCreateRequest));
		return response;
	}

	/**
	 * Generate a random string with the prefix "random"
	 * 
	 * @return
	 */
	protected String randomName() {
		return "random" + System.currentTimeMillis();
	}

	public MeshContainer addSlave(String string, String name, String name2, boolean b) {
		return addSlave(string, name, name2, b, -1);
	}

	/**
	 * Add a new slave and block until the slave is ready.
	 * 
	 * @param clusterName
	 *            Name of the cluster for the slave
	 * @param nodeName
	 *            Name of the slave instance
	 * @param dataPathPostfix
	 *            Prefix used for data storage
	 * @param clearFolders
	 *            Whether to clear any existing data folder of the slave
	 * @param writeQuorum
	 *            Write quorum to be used for the configuration.
	 * @return
	 */
	protected MeshContainer addSlave(String clusterName, String nodeName, String dataPathPostfix, boolean clearFolders, int writeQuorum) {
		MeshContainer server = new MeshContainer(MeshContainer.LOCAL_PROVIDER)
			.withDataPathPostfix(dataPathPostfix)
			.withClusterName(clusterName)
			.withNodeName(nodeName)
			.withFilesystem()
			.withWriteQuorum(writeQuorum)
			.waitForStartup();
		if (clearFolders) {
			server.withClearFolders();
		}
		server.start();
		return server;
	}

}
