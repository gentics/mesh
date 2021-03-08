package com.gentics.mesh.core.binary.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Optional;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.MeshRestClientConfig;

@Ignore
public class FileUploadClientTest {

	public static final String PROJECT_NAME = "test";

	@Test
	public void testUpload() throws IOException {
		MeshRestClientConfig config = MeshRestClientConfig.newConfig().setHost("mesh.test.gentics.com").setPort(80).setSsl(false).build();
		MeshRestClient client = MeshRestClient.create(config);

		// 1. Login
		client.setLogin("admin", "admin");
		client.login().blockingGet();

		Optional<ProjectResponse> op = client.findProjects().blockingGet().getData().stream().filter(p -> p.getName().equalsIgnoreCase(PROJECT_NAME))
			.findFirst();
		if (op.isPresent()) {
			client.deleteProject(op.get().getUuid()).blockingAwait();
		}

		// 2. Create project
		ProjectCreateRequest projectRequest = new ProjectCreateRequest();
		projectRequest.setName(PROJECT_NAME);
		projectRequest.setSchemaRef("folder");
		ProjectResponse project = client.createProject(projectRequest).blockingGet();

		// 3. Get binary content schema uuid
		SchemaResponse schemaResponse = client.findSchemas().blockingGet().getData().stream()
			.filter(schema -> schema.getName().equalsIgnoreCase("binary_content"))
			.findFirst().get();
		String binaryContentSchemaUuid = schemaResponse.getUuid();

		// 4. Assign schema to project
		client.assignSchemaToProject(PROJECT_NAME, binaryContentSchemaUuid).blockingGet();

		File folder = new File("/media/ext4/tmp/dbfiles");
		for (File file : folder.listFiles()) {

			try {
				// 5. Create node
				NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
				nodeCreateRequest.setLanguage("en");
				nodeCreateRequest.setSchemaName("binary_content");
				nodeCreateRequest.setParentNodeUuid(project.getRootNode().getUuid());
				NodeResponse binaryNode = client.createNode(PROJECT_NAME, nodeCreateRequest).blockingGet();

				// 6. Upload to binary field
				String nodeUuid = binaryNode.getUuid();
				FileInputStream ins = new FileInputStream(file);
				try {
					long len = file.length();
					client.updateNodeBinaryField(PROJECT_NAME, nodeUuid, "en", "draft", "binary", ins, len, file.getName(), "application/pdf")
						.blockingGet();
				} finally {
					ins.close();
				}
			} catch (Throwable t) {
				System.out.println("Failure creating node for " + file.getName());
				t.printStackTrace();
			}
		}
	}
}


// 7156.bin