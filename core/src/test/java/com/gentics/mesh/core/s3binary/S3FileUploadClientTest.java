package com.gentics.mesh.core.s3binary;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Optional;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.PublishStatusResponse;
import com.gentics.mesh.core.rest.node.field.s3binary.S3BinaryUploadRequest;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.MeshRestClientConfig;

@Ignore("This is not a read CI-ready UT, but rather an impl testing helper.")
public class S3FileUploadClientTest {

	public static final String PROJECT_NAME = "test";

	@Test
	public void testUpload() throws IOException {
		MeshRestClientConfig config = MeshRestClientConfig.newConfig().setHost("localhost").setPort(8080).setSsl(false).build();
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
			.filter(schema -> schema.getName().equalsIgnoreCase("content"))
			.findFirst().get();
		String binaryContentSchemaUuid = schemaResponse.getUuid();

		// 4. Assign schema to project
		client.assignSchemaToProject(PROJECT_NAME, binaryContentSchemaUuid).blockingGet();
		String filename = "test.jpg";

		try {
			// 5. Create node
			NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
			nodeCreateRequest.setLanguage("en");
			nodeCreateRequest.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
			nodeCreateRequest.setSchemaName("content");
			nodeCreateRequest.getFields().put("slug", FieldUtil.createStringField("folder" + 1));
			nodeCreateRequest.getFields().put("title", FieldUtil.createStringField("folder" + 2));
			nodeCreateRequest.getFields().put("teaser", FieldUtil.createStringField("folder" + 2));
			nodeCreateRequest.setParentNodeUuid(project.getRootNode().getUuid());
			NodeResponse binaryNode = client.createNode(PROJECT_NAME, nodeCreateRequest).blockingGet();
			PublishStatusResponse publishStatusResponse = client.publishNode(PROJECT_NAME, binaryNode.getUuid()).blockingGet();

			assertTrue(publishStatusResponse.getAvailableLanguages().size() > 0);
			// 6. Upload to binary field
			String nodeUuid = binaryNode.getUuid();

			S3BinaryUploadRequest request = new S3BinaryUploadRequest().setFilename(filename).setLanguage("en").setVersion("1.0");

			client.updateNodeS3BinaryField(PROJECT_NAME, nodeUuid, "en", request)
					.blockingGet();
		} catch (Throwable t) {
			System.out.println("Failure creating node for " + filename);
			t.printStackTrace();
		}
	}
}