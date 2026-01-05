package com.gentics.mesh.database;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.dao.RoleDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.hibernate.util.QueryCounter;
import com.gentics.mesh.parameter.client.VersioningParametersImpl;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.ResetTestDb;
import com.gentics.mesh.test.TestSize;
import com.jayway.jsonpath.JsonPath;

import io.vertx.core.json.JsonObject;

@MeshTestSetting(testSize = TestSize.PROJECT, monitoring = true, startServer = true, customOptionChanger = QueryCounter.EnableHibernateStatistics.class, resetBetweenTests = ResetTestDb.NEVER)
public class GraphQLBreadcrumbQueryCountingTest2 extends AbstractCountingTest {
	public final static int NUM_NESTED_FOLDERS = 20;

	protected static String testNodeUuid;

	protected static List<String> testBreadcrumbUuids = new ArrayList<>();

	protected static List<String> testBreadcrumbSlugs = new ArrayList<>();

	protected static String testDraftPermNodeUuid;

	protected static List<String> testDraftPermBreadcrumb = new ArrayList<>();

	protected static String testPublishedPermNodeUuid;

	protected static List<String> testPublishedPermBreadcrumb = new ArrayList<>();

	@Before
	public void setup() {
		if (getTestContext().needsSetup()) {
			// first create a deeply nested folder structure
			String baseNodeUuid = tx(tx -> {
				return tx.projectDao().findByName(PROJECT_NAME).getBaseNode().getUuid();
			});

			AtomicReference<String> parentNodeUuid = new AtomicReference<>(baseNodeUuid);
			testBreadcrumbUuids.add(baseNodeUuid);
			testBreadcrumbSlugs.add(null);
			NodeResponse nodeResponse;

			// create some additional nodes
			for (int i = 0; i < NUM_NESTED_FOLDERS; i++) {
				nodeResponse = createFolder(parentNodeUuid.get(), false, false, true, true);
				parentNodeUuid.set(nodeResponse.getUuid());
				testBreadcrumbUuids.add(parentNodeUuid.get());
				testBreadcrumbSlugs.add(nodeResponse.getFields().getStringField("slug").getString());
			}

			testNodeUuid = parentNodeUuid.get();

			// now create a folder structure with different permissions (drafts)
			parentNodeUuid.set(baseNodeUuid);
			testDraftPermBreadcrumb.add(baseNodeUuid);

			for (boolean readPerm : Arrays.asList(true, false)) {
				for (boolean readPublishedPerm : Arrays.asList(true, false)) {
					nodeResponse = createFolder(parentNodeUuid.get(), false, false, readPerm, readPublishedPerm);
					parentNodeUuid.set(nodeResponse.getUuid());

					if (readPerm) {
						testDraftPermBreadcrumb.add(parentNodeUuid.get());
					}
				}
			}

			nodeResponse = createFolder(parentNodeUuid.get(), false, false, true, true);
			parentNodeUuid.set(nodeResponse.getUuid());
			testDraftPermBreadcrumb.add(parentNodeUuid.get());
			testDraftPermNodeUuid = parentNodeUuid.get();

			// now create a folder structure with different permissions (published)
			parentNodeUuid.set(baseNodeUuid);
			testPublishedPermBreadcrumb.add(baseNodeUuid);

			for (boolean modified : Arrays.asList(true, false)) {
				for (boolean readPerm : Arrays.asList(true, false)) {
					for (boolean readPublishedPerm : Arrays.asList(true, false)) {
						nodeResponse = createFolder(parentNodeUuid.get(), true, modified, readPerm, readPublishedPerm);
						parentNodeUuid.set(nodeResponse.getUuid());

						if (readPerm || readPublishedPerm) {
							testPublishedPermBreadcrumb.add(parentNodeUuid.get());
						}
					}
				}
			}

			nodeResponse = createFolder(parentNodeUuid.get(), true, false, true, true);
			parentNodeUuid.set(nodeResponse.getUuid());
			testPublishedPermBreadcrumb.add(parentNodeUuid.get());
			testPublishedPermNodeUuid = parentNodeUuid.get();
		}

		// clear the cache
		adminCall(() -> client().clearCache());
	}

	@Test
	public void testGetUuid() {
		String graphQl = "query ($uuid: String) { node (uuid: $uuid) { breadcrumb(lang: [\"en\"]) {uuid}} }";

		GraphQLRequest request = new GraphQLRequest();
		request.setQuery(graphQl);
		JsonObject vars = new JsonObject();
		vars.put("uuid", testNodeUuid);
		request.setVariables(vars);

		GraphQLResponse response = doTest(() -> client().graphql(PROJECT_NAME, request), 20);
		List<String> uuids = JsonPath.read(response.toJson(), "$.data.node.breadcrumb[*].uuid");
		assertThat(uuids).as("Uuids in breadcrumb").containsExactlyElementsOf(testBreadcrumbUuids);
	}

	@Test
	public void testGetFields() {
		String graphQl = "query ($uuid: String) { node (uuid: $uuid) { breadcrumb(lang: [\"en\"]) { node { ... on folder {fields { slug }}}}} }";

		GraphQLRequest request = new GraphQLRequest();
		request.setQuery(graphQl);
		JsonObject vars = new JsonObject();
		vars.put("uuid", testNodeUuid);
		request.setVariables(vars);

		GraphQLResponse response = doTest(() -> client().graphql(PROJECT_NAME, request), 20);
		List<String> slugs = JsonPath.read(response.toJson(), "$.data.node.breadcrumb[*].node.fields.slug");
		assertThat(slugs).as("Slugs in breadcrumb").containsExactlyElementsOf(testBreadcrumbSlugs);
	}

	@Test
	public void testDraftPermissions() {
		String graphQl = "query ($uuid: String) { node (uuid: $uuid) { breadcrumb(lang: [\"en\"]) {uuid}} }";

		GraphQLRequest request = new GraphQLRequest();
		request.setQuery(graphQl);
		JsonObject vars = new JsonObject();
		vars.put("uuid", testDraftPermNodeUuid);
		request.setVariables(vars);

		GraphQLResponse response = doTest(() -> client().graphql(PROJECT_NAME, request, new VersioningParametersImpl().draft()), 20);
		List<String> uuids = JsonPath.read(response.toJson(), "$.data.node.breadcrumb[*].uuid");
		assertThat(uuids).as("Uuids in breadcrumb").containsExactlyElementsOf(testDraftPermBreadcrumb);
	}

	@Test
	public void testPublishedPermissions() {
		String graphQl = "query ($uuid: String) { node (uuid: $uuid) { breadcrumb(lang: [\"en\"]) {uuid}} }";

		GraphQLRequest request = new GraphQLRequest();
		request.setQuery(graphQl);
		JsonObject vars = new JsonObject();
		vars.put("uuid", testPublishedPermNodeUuid);
		request.setVariables(vars);

		GraphQLResponse response = doTest(() -> client().graphql(PROJECT_NAME, request, new VersioningParametersImpl().published()), 20);
		List<String> uuids = JsonPath.read(response.toJson(), "$.data.node.breadcrumb[*].uuid");
		assertThat(uuids).as("Uuids in breadcrumb").containsExactlyElementsOf(testPublishedPermBreadcrumb);
	}

	protected NodeResponse createFolder(String parentUuid, boolean published, boolean modified, boolean readPerm, boolean readPublishedPerm) {
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setParentNode(new NodeReference().setUuid(parentUuid));
		nodeCreateRequest.setSchema(new SchemaReferenceImpl().setName("folder"));
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.getFields().put("slug", new StringFieldImpl().setString(RandomStringUtils.secure().nextAlphabetic(5)));
		nodeCreateRequest.setPublish(published);
		NodeResponse nodeResponse = call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest, new NodeParametersImpl().setLanguages("en")));
		String uuid = nodeResponse.getUuid();

		if (modified) {
			NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
			nodeUpdateRequest.setLanguage("en");
			nodeUpdateRequest.setVersion("draft");
			nodeUpdateRequest.getFields().put("slug", new StringFieldImpl().setString(RandomStringUtils.secure().nextAlphabetic(5)));
			nodeResponse = call(() -> client().updateNode(PROJECT_NAME, uuid, nodeUpdateRequest));
		}

		// set/revoke permissions
		tx((tx) -> {
			RoleDao roleDao = tx.roleDao();
			NodeDao nodeDao = tx.nodeDao();

			HibNode node = nodeDao.findByUuid(project(), uuid);
			if (readPerm) {
				roleDao.grantPermissions(role(), node, READ_PERM);
			} else {
				roleDao.revokePermissions(role(), node, READ_PERM);
			}
			if (readPublishedPerm) {
				roleDao.grantPermissions(role(), node, READ_PUBLISHED_PERM);
			} else {
				roleDao.revokePermissions(role(), node, READ_PUBLISHED_PERM);
			}
		});

		return nodeResponse;
	}
}
