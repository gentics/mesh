package com.gentics.mesh.distributed;

import static com.gentics.mesh.test.ClientHelper.call;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.Condition;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.test.context.MeshTestContext;
import com.gentics.mesh.test.context.MeshTestContext.MeshTestInstance;

/**
 * Abstract base class for clustering tests.
 */
public abstract class AbstractMeshClusteringTest {
	@Rule
	@ClassRule
	public static MeshTestContext testContext = new MeshTestContext();

	/**
	 * Map of mesh instances per node name
	 */
	protected static Map<String, MeshTestInstance> instancePerName = new HashMap<>();

	@Before
	public void setup() {
		for (MeshTestInstance instance : testContext.getInstances()) {
			instance.getHttpClient().setLogin("admin", "admin").login().blockingGet();

			instancePerName.put(instance.getOptions().getNodeName(), instance);
		}
	}

	/**
	 * Get the instance with given index.
	 * @param index instance index
	 * @return instance
	 */
	protected MeshTestInstance getInstance(int index) {
		assertThat(testContext.getInstances()).areAtLeast(index,
				new Condition<MeshTestInstance>(inst -> inst != null, "not null"));
		return testContext.getInstances().get(index);
	}

	/**
	 * Get the name of the instance with given index
	 * @param index instance index
	 * @return instance name
	 */
	protected String getInstanceName(int index) {
		assertThat(testContext.getInstances()).areAtLeast(index,
				new Condition<MeshTestInstance>(inst -> inst != null, "not null"));
		return testContext.getInstances().get(index).getOptions().getNodeName();
	}

	protected static String randomName() {
		return RandomStringUtils.randomAlphabetic(10);
	}

	protected static NodeResponse createProjectAndNode(MeshRestClient client, String projectName) {

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
}
