package com.gentics.mesh.core.node;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.PROJECT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.dao.RoleDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.core.rest.navigation.NavigationResponse;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.tag.TagCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.parameter.client.VersioningParametersImpl;
import com.gentics.mesh.test.ElasticsearchTestMode;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.jayway.jsonpath.JsonPath;

import io.vertx.core.json.JsonObject;

/**
 * Test cases for getting nodes with different permissions and publish states
 */
@RunWith(Parameterized.class)
@MeshTestSetting(testSize = PROJECT, startServer = true, elasticsearch = ElasticsearchTestMode.CONTAINER_ES7)
public class NodeReadPermissionTest extends AbstractMeshTest {
	/**
	 * Provide test variations
	 * @return test variations
	 */
	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> paramData() {
		return Arrays.asList(new Object[][] { { "draft" }, { "published" } });
	}

	@Parameter(0)
	public String version;

	private ProjectResponse project;

	private NodeResponse testFolder;

	private NodeResponse newFolder;

	private NodeResponse newFolderWithRead;

	private NodeResponse publishedFolder;

	private NodeResponse publishedFolderWithRead;

	private NodeResponse modifiedFolder;

	private NodeResponse modifiedFolderWithRead;

	private NodeResponse newFolderWithoutPerm;

	private NodeResponse publishedFolderWithoutPerm;

	private NodeResponse modifiedFolderWithoutPerm;

	private TagFamilyResponse tagFamily;

	private TagResponse tag;

	/**
	 * Prepare test data
	 * @throws Exception 
	 */
	@Before
	public void prepareData() throws Exception {
		project = call(() -> client().findProjectByName(PROJECT_NAME));

		tagFamily = call(() -> client().createTagFamily(PROJECT_NAME, new TagFamilyCreateRequest().setName("colors")));
		tag = call(() -> client().createTag(PROJECT_NAME, tagFamily.getUuid(), new TagCreateRequest().setName("red")));

		testFolder = createFolder(project.getRootNode().getUuid(), "Test Folder", false);
		testFolder = publish(testFolder);

		newFolderWithoutPerm = createFolder(testFolder.getUuid(), "New without perm", true);
		newFolder = createFolder(testFolder.getUuid(), "New", true);
		newFolderWithRead = createFolder(testFolder.getUuid(), "New with read", true);

		publishedFolderWithoutPerm = createFolder(testFolder.getUuid(), "Published without perm", true);
		publishedFolderWithoutPerm = publish(publishedFolderWithoutPerm);
		publishedFolder = createFolder(testFolder.getUuid(), "Published", true);
		publishedFolder = publish(publishedFolder);
		publishedFolderWithRead = createFolder(testFolder.getUuid(), "Published with read", true);
		publishedFolderWithRead = publish(publishedFolderWithRead);

		modifiedFolderWithoutPerm = createFolder(testFolder.getUuid(), "Original without perm", true);
		modifiedFolderWithoutPerm = publish(modifiedFolderWithoutPerm);
		modifiedFolderWithoutPerm = update(modifiedFolderWithoutPerm, "Modified without perm");
		modifiedFolder = createFolder(testFolder.getUuid(), "Original", true);
		modifiedFolder = publish(modifiedFolder);
		modifiedFolder = update(modifiedFolder, "Modified");
		modifiedFolderWithRead = createFolder(testFolder.getUuid(), "Original with read", true);
		modifiedFolderWithRead = publish(modifiedFolderWithRead);
		modifiedFolderWithRead = update(modifiedFolderWithRead, "Modified with read");

		setPermission(newFolderWithoutPerm, null);
		setPermission(newFolder, READ_PUBLISHED_PERM);
		setPermission(newFolderWithRead, READ_PERM);
		setPermission(publishedFolderWithoutPerm, null);
		setPermission(publishedFolder, READ_PUBLISHED_PERM);
		setPermission(publishedFolderWithRead, READ_PERM);
		setPermission(modifiedFolderWithoutPerm, null);
		setPermission(modifiedFolder, READ_PUBLISHED_PERM);
		setPermission(modifiedFolderWithRead, READ_PERM);

		recreateIndices();
	}

	/**
	 * Test calling findNodes() to get all nodes
	 */
	@Test
	public void testFindNodes() {
		NodeListResponse nodeList = call(() -> client().findNodes(PROJECT_NAME, new VersioningParametersImpl().setVersion(version)));
		List<String> folderNames = nodeList.getData().stream().map(NodeResponse::getDisplayName).collect(Collectors.toList());
		assertFolderNames(folderNames, true);
	}

	/**
	 * Test calling findNodeChildren() to get all child nodes
	 */
	@Test
	public void testFindNodeChildren() {
		NodeListResponse nodeList = call(() -> client().findNodeChildren(PROJECT_NAME, testFolder.getUuid(),
				new VersioningParametersImpl().setVersion(version)));
		List<String> folderNames = nodeList.getData().stream().map(NodeResponse::getDisplayName).collect(Collectors.toList());
		assertFolderNames(folderNames, false);
	}

	/**
	 * Test calling findNodesForTag() to get nodes with given tag
	 */
	@Test
	public void testFindNodesForTag() {
		NodeListResponse nodeList = call(() -> client().findNodesForTag(PROJECT_NAME, tagFamily.getUuid(), tag.getUuid(),
				new VersioningParametersImpl().setVersion(version)));
		List<String> folderNames = nodeList.getData().stream().map(NodeResponse::getDisplayName).collect(Collectors.toList());
		assertFolderNames(folderNames, false);
	}

	/**
	 * Test searchNodes (globally)
	 */
	@Test
	public void testSearchNodes() {
		NodeListResponse nodeList = call(() -> client().searchNodes(
				"{ \"query\": { \"match\": { \"schema.name\": { \"query\": \"folder\" } } } }",
				new VersioningParametersImpl().setVersion(version)));
		List<String> folderNames = nodeList.getData().stream().map(NodeResponse::getDisplayName)
				.collect(Collectors.toList());
		assertFolderNames(folderNames, true);
	}

	/**
	 * Test searchNodes (in project)
	 */
	@Test
	public void testSearchNodesInProject() {
		NodeListResponse nodeList = call(() -> client().searchNodes(PROJECT_NAME,
				"{ \"query\": { \"match\": { \"schema.name\": { \"query\": \"folder\" } } } }",
				new VersioningParametersImpl().setVersion(version)));
		List<String> folderNames = nodeList.getData().stream().map(NodeResponse::getDisplayName)
				.collect(Collectors.toList());
		assertFolderNames(folderNames, true);
	}

	/**
	 * Test loadNavigation
	 */
	@Test
	public void testLoadNavigation() {
		NavigationResponse navigation = call(() -> client().loadNavigation(PROJECT_NAME, testFolder.getUuid(),
				new VersioningParametersImpl().setVersion(version)));
		assertThat(navigation.getChildren()).as("Navigation children").isNotNull();
		List<String> folderNames = navigation.getChildren().stream().map(item -> item.getNode().getDisplayName())
				.collect(Collectors.toList());
		assertFolderNames(folderNames, false);
	}

	/**
	 * Test calling navroot for the Test folder
	 */
	@Test
	public void testNavroot() {
		NavigationResponse navigation = call(() -> client().navroot(PROJECT_NAME, "/Test%20Folder",
				new VersioningParametersImpl().setVersion(version)));
		assertThat(navigation.getChildren()).as("Navigation children").isNotNull();
		List<String> folderNames = navigation.getChildren().stream().map(item -> item.getNode().getDisplayName())
				.collect(Collectors.toList());
		assertFolderNames(folderNames, false);
	}

	/**
	 * Test getting nodes via graphQL
	 */
	@Test
	public void testGraphQLNodes() {
		doGraphQLTest("{nodes {elements {displayName }}}", "$.data.nodes.elements[*].displayName", true);
	}

	/**
	 * Test getting node children via graphql
	 */
	@Test
	public void testGraphQLChildren() {
		doGraphQLTest("query ($uuid: String) { node(uuid: $uuid) { uuid children { elements { displayName}}}}",
				"$.data.node.children.elements[*].displayName", false, "uuid", testFolder.getUuid());
	}

	/**
	 * Test getting nodes of schema via graphql
	 */
	@Test
	public void testGraphQLNodesOfSchema() {
		doGraphQLTest("query ($schema: String) { schema(name: $schema) { nodes { elements { displayName } } } }",
				"$.data.schema.nodes.elements[*].displayName", true, "schema", "folder");
	}

	/**
	 * Test getting nodes with tag via graphql
	 */
	@Test
	public void testGraphQLNodesWithTag() {
		doGraphQLTest("{ tag(name:\"red\") { nodes { elements { displayName } } } }",
				"$.data.tag.nodes.elements[*].displayName", false);
	}

	/**
	 * Test searching for nodes via graphql
	 */
	@Test
	public void testGraphQLSearchNodes() {
		doGraphQLTest("query ($search: String) { nodes(query: $search) { elements { displayName } } }",
				"$.data.nodes.elements[*].displayName", true, "search",
				"{ \"query\": { \"match\": { \"schema.name\": { \"query\": \"folder\" } } } }");
	}

	/**
	 * Perform a graphQL test
	 * @param query graphQL query
	 * @param path Json Path to extract the display names of the returned nodes from the graphQL response
	 * @param includeOthers true if other nodes (test folder and already existing folders) are expected to be included in the result
	 * @param varsKeyValues optional key/value pairs of graphQL variables
	 */
	protected void doGraphQLTest(String query, String path, boolean includeOthers, String...varsKeyValues) {
		GraphQLRequest request = new GraphQLRequest();
		request.setQuery(query);

		if (varsKeyValues.length > 0) {
			JsonObject vars = new JsonObject();
			for (int i = 0; i < varsKeyValues.length; i += 2) {
				vars.put(varsKeyValues[i], varsKeyValues[i + 1]);
			}
			request.setVariables(vars);
		}
		GraphQLResponse graphQLResponse = call(
				() -> client().graphql(PROJECT_NAME, request, new VersioningParametersImpl().setVersion(version)));
		List<String> folderNames = JsonPath.read(graphQLResponse.toJson(), path);
		assertFolderNames(folderNames, includeOthers);
	}

	/**
	 * Assert that the given folder display names contain only the expected display names
	 * @param folderNames given folder diplay names
	 * @param includeOthers true if other nodes (test folder and already existing folders) are expected to be included in the list
	 */
	protected void assertFolderNames(List<String> folderNames, boolean includeOthers) {
		folderNames.removeIf(name -> name == null);
		switch (version) {
		case "draft":
			if (includeOthers) {
				assertThat(folderNames).as("Folder names").containsOnly(
						"2015",
						"News",
						"Test Folder",
						"New with read",
						"Published with read",
						"Modified with read");
			} else {
				assertThat(folderNames).as("Folder names").containsOnly(
						"New with read",
						"Published with read",
						"Modified with read");
			}
			break;
		case "published":
			if (includeOthers) {
				assertThat(folderNames).as("Folder names").containsOnly(
						"2015",
						"News",
						"Test Folder",
						"Published",
						"Published with read",
						"Original",
						"Original with read");
			} else {
				assertThat(folderNames).as("Folder names").containsOnly(
						"Published",
						"Published with read",
						"Original",
						"Original with read");
			}
			break;
		default:
			fail("Unexpected version " + version);
		}
	}

	/**
	 * Create a new folder
	 * @param parentNodeUuid parent node UUID
	 * @param name folder name (wil be the display name)
	 * @param setTag true to set the tag on the folder
	 * @return created folder
	 */
	protected NodeResponse createFolder(String parentNodeUuid, String name, boolean setTag) {
		NodeCreateRequest createRequest = new NodeCreateRequest();
		createRequest.setSchemaName("folder");
		createRequest.setParentNodeUuid(parentNodeUuid);
		createRequest.setLanguage("en");
		createRequest.getFields().put("name", FieldUtil.createStringField(name));
		createRequest.getFields().put("slug", FieldUtil.createStringField(name));
		NodeResponse response = call(() -> client().createNode(PROJECT_NAME, createRequest));
		if (setTag) {
			call(() -> client().addTagToNode(PROJECT_NAME, response.getUuid(), tag.getUuid()));
		}
		return response;
	}

	/**
	 * Publish the given node
	 * @param node node to publish
	 * @return node
	 */
	protected NodeResponse publish(NodeResponse node) {
		call(() -> client().publishNode(PROJECT_NAME, node.getUuid()));
		return node;
	}

	/**
	 * Update the given folder by setting the given new name
	 * @param node folder to update
	 * @param newName new name
	 * @return updated node
	 */
	protected NodeResponse update(NodeResponse node, String newName) {
		NodeUpdateRequest updateRequest = new NodeUpdateRequest();
		updateRequest.setLanguage("en");
		updateRequest.setVersion("draft");
		updateRequest.getFields().put("name", FieldUtil.createStringField(newName));
		return call(() -> client().updateNode(PROJECT_NAME, node.getUuid(), updateRequest));
	}

	/**
	 * Set the given permission on the node. First read and read_published are revoked, then the given perm (if not null) is granted
	 * @param node node
	 * @param perm permission to grant (may be null)
	 */
	protected void setPermission(NodeResponse node, InternalPermission perm) {
		tx(tx -> {
			NodeDao nodeDao = tx.nodeDao();
			RoleDao roleDao = tx.roleDao();
			HibNode n = nodeDao.findByUuid(project(), node.getUuid());
			roleDao.revokePermissions(role(), n, READ_PERM, READ_PUBLISHED_PERM);
			if (perm != null) {
				roleDao.grantPermissions(role(), n, perm);
			}
		});
	}
}
