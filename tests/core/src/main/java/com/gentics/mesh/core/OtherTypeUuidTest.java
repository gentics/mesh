package com.gentics.mesh.core;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.TRACKING;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.vertx.core.json.JsonObject;

/**
 * Tests that the findByUuid methods return 404 if a UUID of an entity of another type is provided.
 */
@MeshTestSetting(elasticsearch = TRACKING, testSize = FULL, startServer = true)
public class OtherTypeUuidTest extends AbstractMeshTest {

	private String colorsUuid;
	private ProjectResponse otherProject;
	private String planeUuid;

	@Before
	public void setUp() throws Exception {
		colorsUuid = tx(() -> tagFamily("colors").getUuid());
		planeUuid = tx(() -> tag("plane").getUuid());
		otherProject = createProject("testProject");
	}

	@Test
	public void testNode() {
		expect404(client().findNodeByUuid(PROJECT_NAME, projectUuid()), projectUuid());
	}

	@Test
	public void testNodeFromOtherProject() {
		expect404(client().findNodeByUuid(otherProject.getName(), folderUuid()));
	}

	@Test
	public void testProject() {
		expect404(client().findProjectByUuid(folderUuid()));
	}

	@Test
	public void testSchema() {
		expect404(client().findSchemaByUuid(folderUuid()));
	}

	@Test
	public void testMicroschema() {
		expect404(client().findMicroschemaByUuid(folderUuid()));
	}

	@Test
	public void testTagFamily() {
		expect404(client().findTagFamilyByUuid(PROJECT_NAME, folderUuid()));
	}

	@Test
	public void testTagFamilyFromOtherProject() {
		expect404(client().findTagFamilyByUuid(otherProject.getName(), colorsUuid), colorsUuid);
	}

	@Test
	public void testTag() {
		expect404(client().findTagByUuid(PROJECT_NAME, colorsUuid, folderUuid()));
	}
	@Test
	public void testTagFromOtherFamily() {
		expect404(client().findTagByUuid(PROJECT_NAME, colorsUuid, planeUuid), planeUuid);
	}

	@Test
	public void testUser() {
		expect404(client().findUserByUuid(folderUuid()));
	}

	@Test
	public void testGroup() {
		expect404(client().findGroupByUuid(folderUuid()));
	}

	@Test
	public void testRole() {
		expect404(client().findRoleByUuid(folderUuid()));
	}

	@Test
	public void testLinkRendering() {
		String resolvedLink = client().resolveLinks("{{mesh.link('" + projectUuid() + "')}}",
			new NodeParametersImpl().setResolveLinks(LinkType.SHORT)).blockingGet();
		assertThat(resolvedLink).isEqualTo("/error/404");
	}

	@Test
	public void testGraphQL() {
		GraphQLRequest request = new GraphQLRequest()
			.setQuery("query nodeByUuid($uuid:String) {\n" +
				"  node(uuid: $uuid) {\n" +
				"    uuid\n" +
				"  }\n" +
				"}")
			.setVariables(new JsonObject().put("uuid", projectUuid()));
		GraphQLResponse graphQLResponse = client().graphql(PROJECT_NAME, request).blockingGet();
		assertThat(graphQLResponse.getData().getJsonObject("node")).isNull();
	}

	private void expect404(MeshRequest<?> request) {
		expect404(request, folderUuid());
	}

	private void expect404(MeshRequest<?> request, String uuid) {
		call(() -> request, NOT_FOUND, "object_not_found_for_uuid", uuid);
	}
}
