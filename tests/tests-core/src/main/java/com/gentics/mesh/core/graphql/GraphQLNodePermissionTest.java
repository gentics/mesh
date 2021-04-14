package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.graphql.GraphQLNodePermissionTest.ContentSetupType.NO_PUBLISH_SELECT_DRAFT;
import static com.gentics.mesh.core.graphql.GraphQLNodePermissionTest.ContentSetupType.PUBLISH_SELECT_DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.common.Permission;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.core.rest.role.RolePermissionRequest;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;

import io.vertx.core.json.JsonObject;

/**
 * This test asserts the node permissions at all places where nodes are being used in GraphQL. The variations of this test are used to validate cases for the
 * parameters:
 * <ul>
 * <li>Permissions on node (None, READ, READ_PUBLISHED)</li>
 * <li>Content Setup (variations of publishing and content selection)</li>
 * </ul>
 */
@RunWith(Parameterized.class)
@MeshTestSetting(testSize = TestSize.FULL, startServer = true)
public class GraphQLNodePermissionTest extends AbstractGraphQLNodeTest {

	private PermissionScenario perm;
	private ContentSetupType setup;

	public GraphQLNodePermissionTest(PermissionScenario perm, ContentSetupType setup) {
		this.perm = perm;
		this.setup = setup;
	}

	/**
	 * permission: Permission to be granted on the nodes
	 * 
	 * content: The content setup for the test (e.g. whether to publish the node under test)
	 * 
	 * @return
	 */
	@Parameters(name = "permission={0},content={1}")
	public static Collection<Object[]> paramData() {
		List<Object[]> data = new ArrayList<>();
		// Generate permutations
		for (PermissionScenario perm : PermissionScenario.values()) {
			for (ContentSetupType content : ContentSetupType.values()) {
				data.add(new Object[] { perm, content });
			}
		}
		return data;
	}

	@Test
	public void testPermissions() throws IOException {
		switch (setup) {
		case PUBLISH_SELECT_DRAFT:
		case PUBLISH_SELECT_PUBLISHED:
			setupContents(true);
			break;
		case NO_PUBLISH_SELECT_DRAFT:
		case NO_PUBLISH_SELECT_PUBLISHED:
			setupContents(false);
			break;
		}
		applyVariations();

		// Now execute the query and assert it
		GraphQLRequest request = new GraphQLRequest();
		JsonObject vars = new JsonObject();
		ContainerType selectedType = (setup == PUBLISH_SELECT_DRAFT || setup == NO_PUBLISH_SELECT_DRAFT) ? DRAFT : PUBLISHED;
		vars.put("type", selectedType.getHumanCode());
		vars.put("invType", selectedType == DRAFT ? PUBLISHED.getHumanCode() : DRAFT.getHumanCode());

		request.setVariables(vars);

		for (TestQuery field : TestQuery.values()) {
			String queryName = "node/permissions/" + field.path();
			request.setQuery(getGraphQLQuery(queryName));
			GraphQLResponse response = call(() -> client().graphql(PROJECT_NAME, request));
			JsonObject jsonResponse = new JsonObject(response.toJson());
			System.out.println(jsonResponse.encodePrettily());
			System.out.println("Query: " + queryName);
			compliesToAssertions(jsonResponse, queryName);
			assertThat(jsonResponse).hasNoGraphQLSyntaxError();
		}
	}

	public static final String CHECK_PERM = "checkperm:";

	private void compliesToAssertions(JsonObject jsonResponse, String queryName) throws IOException {
		String query = getGraphQLQuery(queryName);

		try (Scanner scanner = new Scanner(query)) {
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				line = line.trim();
				if (line.startsWith("# [")) {
					int start = line.indexOf("# [") + 3;
					int end = line.lastIndexOf("]");
					String selector = line.substring(start, end);
					int ab = line.indexOf("=", end) + 1;
					String assertion = line.substring(ab);
					String currentSelector = perm.name() + "," + setup.name();
					if (currentSelector.equalsIgnoreCase(selector)) {
						if (assertion.startsWith(CHECK_PERM)) {
							String permPath = assertion.substring(CHECK_PERM.length());
							assertThat(jsonResponse).hasPermFailure(permPath);
						} else {
							assertThat(jsonResponse).compliesTo(assertion);
						}
					}
				}
			}
		}

	}

	private void applyVariations() {
		// Apply permissions for test run
		RolePermissionRequest permRequest = new RolePermissionRequest();
		PermissionInfo permissionsInfo = permRequest.getPermissions();
		switch (perm) {
		case ONLY_READ:
			permissionsInfo.set(Permission.READ, true);
			break;
		case ONLY_READ_PUBLISHED:
			permissionsInfo.set(Permission.READ_PUBLISHED, true);
			break;
		case NO_PERM:
			// Not setting any perms
		}
		permissionsInfo.setOthers(false);
		permRequest.setRecursive(true);
		adminCall(
			() -> client().updateRolePermissions(roleUuid(), "/projects/" + PROJECT_NAME + "/nodes", permRequest));

		// We can't test the scenario without any permissions. We need to grant perm for the node under test.
		// The NODE_1B is used to access node.child or node.children fields. Withouts perms we could not access the fields in this perm scenario.
		if (PermissionScenario.NO_PERM == perm) {
			RolePermissionRequest permRequest2 = new RolePermissionRequest();
			permRequest2.setRecursive(false);
			permRequest2.getPermissions().set(Permission.READ, true);
			permRequest2.getPermissions().setOthers(false);
			adminCall(() -> client().updateRolePermissions(roleUuid(),
				"/projects/" + PROJECT_NAME + "/nodes/" + NODE_1B_UUID, permRequest2));
		}
	}

	public static enum TestQuery {

		ME_NODE_REFERENCE("me.nodeReference"),

		NODE_UUID("nodePerUuid"),

		NODE_UUID_PARENT("nodePerUuid.parent"),

		NODE_UUID_BREADCRUMB("nodePerUuid.breadcrumb"),

		NODE_UUID_LANGUAGES("nodePerUuid.languages"),

		NODE_UUID_FIELDS_NODE("nodePerUuid.fields.node"),

		NODE_UUID_FIELDS_NODE_LIST("nodePerUuid.fields.nodeList"),

		NODE_UUID_CHILD("nodePerUuid.child"),

		NODE_UUID_NOLANG("nodePerUuid.nolang"),

		SCHEMA_NODES("schema.nodes.elements"),

		ROOT_NODE("rootNode"),

		TAG_NODES("tag.nodes.elements"),

		TAGS_NODES("tags.elements[0].nodes.elements"),

		PROJECT_ROOT_NODE("project.rootNode"),

		NODE_UUID_REFERENCED_BY("nodePerUuid.referencedBy.elements"),

		NODE_UUID_CHILDREN("nodePerUuid.children.elements"),

		NODE_PATH("nodePerPath"),

		NODES("nodes.elements"),

		NODE_UUIDS("uuidNodes.elements"),

		NODE_UUID_INVERTED("nodePerUuid.invNodeType"),

		NODE_INVERTED_UUID("nodePerUuidInverted"),

		NODE_INVERTED_UUID_NOLANG("nodePerUuidInverted.nolang"),

		NODE_INVERTED_UUID_PARENT("nodePerUuidInverted.parent"),

		NODE_INVERTED_UUID_LANGUAGES("nodePerUuidInverted.languages"),

		NODE_INVERTED_UUID_CHILD("nodePerUuidInverted.child"),

		NODE_INVERTED_UUID_BREADCRUMB("nodePerUuidInverted.breadcrumb"),

		NODE_INVERTED_UUID_REFERENCED_BY("nodePerUuidInverted.referencedBy.elements"),

		NODE_INVERTED_UUID_CHILDREN("nodePerUuidInverted.children.elements"),

		NODE_INVERTED_UUID_FIELDS_NODE("nodePerUuidInverted.fields.node"),

		NODE_INVERTED_UUID_FIELDS_NODE_LIST("nodePerUuidInverted.fields.nodeList");

		/**
		 * Path to the field.
		 */
		private String path;

		TestQuery(String path) {
			this.path = path;
		}

		String path() {
			return path;
		}

		@Override
		public String toString() {
			return path();
		}

	}

	public static enum ContentSetupType {

		// Expect draft version (1.1)
		PUBLISH_SELECT_DRAFT,

		// Expect published version (1.0)
		PUBLISH_SELECT_PUBLISHED,

		// Expect draft version (0.2)
		NO_PUBLISH_SELECT_DRAFT,

		// Expect no fields (e.g. collections should be empty)
		NO_PUBLISH_SELECT_PUBLISHED;
	}

	public static enum PermissionScenario {
		/**
		 * Grant no read permissions to most nodes. Note that NODE_1B_UUID still got permissions in order to assert nested fields.
		 */
		NO_PERM,

		/**
		 * Grant only READ permissions to all nodes.
		 */
		ONLY_READ,

		/**
		 * Grant only READ_PUBLISHED permissions to all nodes. Draft contents are thus not load-able.
		 */
		ONLY_READ_PUBLISHED
	}

}
