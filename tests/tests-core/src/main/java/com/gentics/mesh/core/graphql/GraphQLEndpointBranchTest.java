package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;

import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;

import io.vertx.core.json.JsonObject;

@MeshTestSetting(testSize = TestSize.FULL, startServer = true)
public class GraphQLEndpointBranchTest extends AbstractMeshTest {
	private final String BRANCH_NAME = "testBranch";

	public void setupData(boolean publish) {
		grantAdmin();
		createBranchRestAndWait(BRANCH_NAME, false);

		NodeResponse content1 = createContent("test1", "test", publish);
		createContent("test2", String.format("{{mesh.link('%s')}}", content1.getUuid()), publish);
	}

	@Test
	public void testLinkResolving() throws IOException {
		setupData(false);
		String queryName = "branch/link-resolving-query";
		GraphQLResponse response = call(() -> client().graphqlQuery(PROJECT_NAME, getGraphQLQuery(queryName), new VersioningParametersImpl().setBranch(BRANCH_NAME)));
		JsonObject json = new JsonObject(response.toJson());
		assertThat(json).compliesToAssertions(queryName);
	}

	@Test
	public void testLinkResolvingDraft() throws IOException {
		setupData(false);
		testLinkResolvingVersion("draft");
	}
	@Test
	public void testLinkResolvingPublished() throws IOException {
		setupData(true);
		testLinkResolvingVersion("published");
	}

	protected void testLinkResolvingVersion(String version) throws IOException {
		String queryName = "branch/link-resolving-query-" + version;
		GraphQLResponse response = call(() -> client().graphqlQuery(PROJECT_NAME, getGraphQLQuery(queryName), new VersioningParametersImpl().setBranch(BRANCH_NAME).setVersion(version)));
		JsonObject json = new JsonObject(response.toJson());
		assertThat(json).compliesToAssertions(queryName);
	}

	private NodeResponse createContent(String slug, String content, boolean publish) {
		ProjectResponse project = getProject();
		NodeCreateRequest request = new NodeCreateRequest();
		request.setParentNodeUuid(project.getRootNode().getUuid());
		request.setSchemaName("content");
		request.setLanguage("en");
		request.setPublish(publish);
		FieldMap fields = request.getFields();
		fields.put("slug", new StringFieldImpl().setString(slug));
		fields.put("teaser", new StringFieldImpl().setString(content));
		fields.put("content", new StringFieldImpl().setString(content));
		return call(() -> client().createNode(PROJECT_NAME, request, new VersioningParametersImpl().setBranch(BRANCH_NAME)));
	}

	private void createBranchRestAndWait(String name, boolean latest) {
		waitForJobs(() -> {
			createBranchRest(name, latest);
		}, JobStatus.COMPLETED, 1);
	}
}
