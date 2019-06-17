package com.gentics.mesh.core.graphql;

import com.gentics.mesh.core.rest.branch.BranchCreateRequest;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import io.vertx.core.json.JsonObject;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;

@MeshTestSetting(testSize = TestSize.FULL, startServer = true)
public class GraphQLEndpointBranchTest extends AbstractMeshTest {
	private final String BRANCH_NAME = "testBranch";

	@Before
	public void setupData() {
		grantAdminRole();
		createBranchRest(BRANCH_NAME, false);

		NodeResponse content1 = createContent("test1", "test");
		createContent("test2", String.format("{{mesh.link('%s')}}", content1.getUuid()));
	}

	@Test
	public void testLinkResolving() throws IOException {
		String queryName = "branch/link-resolving-query";
		GraphQLResponse response = call(() -> client().graphqlQuery(PROJECT_NAME, getGraphQLQuery(queryName), new VersioningParametersImpl().setBranch(BRANCH_NAME)));
		JsonObject json = new JsonObject(response.toJson());
		assertThat(json).compliesToAssertions(queryName);
	}

	private NodeResponse createContent(String slug, String content) {
		ProjectResponse project = getProject();
		NodeCreateRequest request = new NodeCreateRequest();
		request.setParentNodeUuid(project.getRootNode().getUuid());
		request.setSchemaName("content");
		request.setLanguage("en");
		FieldMap fields = request.getFields();
		fields.put("slug", new StringFieldImpl().setString(slug));
		fields.put("teaser", new StringFieldImpl().setString(content));
		fields.put("content", new StringFieldImpl().setString(content));
		return call(() -> client().createNode(PROJECT_NAME, request, new VersioningParametersImpl().setBranch(BRANCH_NAME)));
	}

	private ProjectResponse getProject() {
		return call(() -> client().findProjectByName(PROJECT_NAME));
	}

	private void createBranchRest(String name, boolean latest) {
		BranchCreateRequest request = new BranchCreateRequest();
		request.setName(name);
		request.setLatest(latest);
		waitForJobs(() -> {
			call(() -> client().createBranch(PROJECT_NAME, request));
		}, JobStatus.COMPLETED, 1);
	}
}
