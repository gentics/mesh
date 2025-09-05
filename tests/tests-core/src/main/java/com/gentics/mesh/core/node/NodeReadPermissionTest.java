package com.gentics.mesh.core.node;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.INITIAL_BRANCH_NAME;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.PROJECT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
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
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.core.rest.navigation.NavigationResponse;
import com.gentics.mesh.core.rest.node.NodeChildrenInfo;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.tag.TagCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.parameter.VersioningParameters;
import com.gentics.mesh.parameter.client.NodeParametersImpl;
import com.gentics.mesh.parameter.client.PagingParametersImpl;
import com.gentics.mesh.parameter.client.VersioningParametersImpl;
import com.gentics.mesh.test.ElasticsearchTestMode;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.ResetTestDb;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.jayway.jsonpath.JsonPath;

import io.vertx.core.json.JsonObject;

/**
 * Test cases for getting nodes with different permissions and publish states
 */
@RunWith(Parameterized.class)
@MeshTestSetting(testSize = PROJECT, startServer = true, elasticsearch = ElasticsearchTestMode.CONTAINER_ES7, resetBetweenTests = ResetTestDb.NEVER)
public class NodeReadPermissionTest extends AbstractMeshTest {
	public final static String BRANCH_NAME = "alternative";

	/**
	 * Provide test variations
	 * @return test variations
	 */
	@Parameters(name = "{index}: version: {0}, branch: {1}, english: {2}, german: {3}")
	public static Collection<Object[]> paramData() {
		Collection<Object[]> params = new ArrayList<>();

		for (String version : Arrays.asList("draft", "published")) {
			for (String branch : Arrays.asList(null, INITIAL_BRANCH_NAME, BRANCH_NAME)) {
				for (boolean english : Arrays.asList(true, false)) {
					for (boolean german : Arrays.asList(true, false)) {
						if (!english && !german) {
							continue;
						}
						params.add(new Object[] { version, branch, english, german });
					}
				}
			}
		}
		return params;
	}

	@Parameter(0)
	public String version;

	@Parameter(1)
	public String branch;

	@Parameter(2)
	public boolean english;

	@Parameter(3)
	public boolean german;

	private static ProjectResponse project;

	private static NodeResponse testFolder;

	private static TagFamilyResponse tagFamily;

	private static TagResponse tag;

	private VersioningParameters versioning;

	private ParameterProvider lang;

	/**
	 * Prepare test data
	 * @throws Exception 
	 */
	@Before
	public void prepareData() throws Exception {
		versioning = new VersioningParametersImpl().setVersion(version);
		if (!StringUtils.isEmpty(branch)) {
			versioning.setBranch(branch);
		}
		if (english && german) {
			lang = new NodeParametersImpl().setLanguages("en", "de");
		} else if (english) {
			lang = new NodeParametersImpl().setLanguages("en");
		} else if (german) {
			lang = new NodeParametersImpl().setLanguages("de");
		}

		if (!testContext.needsSetup()) {
			return;
		}
		project = call(() -> client().findProjectByName(PROJECT_NAME));

		tagFamily = call(() -> client().createTagFamily(PROJECT_NAME, new TagFamilyCreateRequest().setName("colors")));
		tag = call(() -> client().createTag(PROJECT_NAME, tagFamily.getUuid(), new TagCreateRequest().setName("red")));

		testFolder = createFolder(project.getRootNode().getUuid(), "Test Folder", "Test Ordner", false, null);
		testFolder = publish(testFolder, null);

		// create common folders
		// new
		createFolders("New without perm", "Neu ohne Recht", null, false, false, null);
		createFolders("New", "Neu", READ_PUBLISHED_PERM, false, false, null);
		createFolders("New with read", "Neu mit Lesen", READ_PERM, false, false, null);
		// published
		createFolders("Published without perm", "Publiziert ohne Recht", null, true, false, null);
		createFolders("Published", "Publiziert", READ_PUBLISHED_PERM, true, false, null);
		createFolders("Published with read", "Publiziert mit Lesen", READ_PERM, true, false, null);
		// modified
		createFolders("Folder without perm", "Ordner ohne Recht", null, true, true, null);
		createFolders("Folder", "Ordner", READ_PUBLISHED_PERM, true, true, null);
		createFolders("Folder with read", "Ordner mit Lesen", READ_PERM, true, true, null);

		// create a second branch
		createBranch(BRANCH_NAME, false);

		// create branch folders
		// new
		createFolders("Alternative New without perm", "Alternativ Neu ohne Recht", null, false, false, BRANCH_NAME);
		createFolders("Alternative New", "Alternativ Neu", READ_PUBLISHED_PERM, false, false, BRANCH_NAME);
		createFolders("Alternative New with read", "Alternativ Neu mit Lesen", READ_PERM, false, false, BRANCH_NAME);
		// published
		createFolders("Alternative Published without perm", "Alternativ Publiziert ohne Recht", null, true, false, BRANCH_NAME);
		createFolders("Alternative Published", "Alternativ Publiziert", READ_PUBLISHED_PERM, true, false, BRANCH_NAME);
		createFolders("Alternative Published with read", "Alternativ Publiziert mit Lesen", READ_PERM, true, false, BRANCH_NAME);
		// modified
		createFolders("Alternative Folder without perm", "Alternativ Ordner ohne Recht", null, true, true, BRANCH_NAME);
		createFolders("Alternative Folder", "Alternativ Ordner", READ_PUBLISHED_PERM, true, true, BRANCH_NAME);
		createFolders("Alternative Folder with read", "Alternativ Ordner mit Lesen", READ_PERM, true, true, BRANCH_NAME);

		recreateIndices();
		waitForSearchIdleEvent();
	}

	protected void createFolders(String nameEn, String nameDe, InternalPermission perm, boolean publish, boolean modify, String branch) {
		// variant in both languages
		NodeResponse tmp = createFolder(testFolder.getUuid(), nameEn, nameDe, true, branch);
		if (publish) {
			publish(tmp, branch);
		}
		if (modify) {
			update(tmp, "Modified " + nameEn, "Modifiziert " + nameDe, branch);
		}
		setPermission(tmp, perm);

		// english only
		tmp = createFolder(testFolder.getUuid(), nameEn + " en", null, true, branch);
		if (publish) {
			publish(tmp, branch);
		}
		if (modify) {
			update(tmp, "Modified " + nameEn + " en", null, branch);
		}
		setPermission(tmp, perm);

		// german only
		tmp = createFolder(testFolder.getUuid(), null, nameDe + " de", true, branch);
		if (publish) {
			publish(tmp, branch);
		}
		if (modify) {
			update(tmp, null, "Modifiziert " + nameDe + " de", branch);
		}
		setPermission(tmp, perm);
	}

	/**
	 * Test calling findNodes() to get all nodes
	 */
	@Test
	public void testFindNodes() {
		NodeListResponse nodeList = call(() -> client().findNodes(PROJECT_NAME, versioning, lang));
		List<String> folderNames = nodeList.getData().stream().map(NodeResponse::getDisplayName).collect(Collectors.toList());
		assertFolderNames(folderNames, true);
	}

	/**
	 * Test calling findNodeChildren() to get all child nodes
	 */
	@Test
	public void testFindNodeChildren() {
		NodeListResponse nodeList = call(() -> client().findNodeChildren(PROJECT_NAME, testFolder.getUuid(),
				versioning, lang));
		List<String> folderNames = nodeList.getData().stream().map(NodeResponse::getDisplayName).collect(Collectors.toList());
		assertFolderNames(folderNames, false);
	}

	/**
	 * Test calling findNodesForTag() to get nodes with given tag
	 */
	@Test
	public void testFindNodesForTag() {
		NodeListResponse nodeList = call(() -> client().findNodesForTag(PROJECT_NAME, tagFamily.getUuid(), tag.getUuid(),
				versioning, lang));
		List<String> folderNames = nodeList.getData().stream().map(NodeResponse::getDisplayName).collect(Collectors.toList());
		assertFolderNames(folderNames, false);
	}

	/**
	 * Test searchNodes (globally)
	 */
	@Test
	public void testSearchNodes() {
		// doing a global search will fail, if we search for published nodes and not in the branch, because the global search
		// will find nodes of the branch, but mesh cannot find it
		if (StringUtils.equals(version, "published") && !StringUtils.equals(branch, BRANCH_NAME)) {
			return;
		}
		NodeListResponse nodeList = call(() -> client().searchNodes(
				"{ \"query\": { \"match\": { \"schema.name\": { \"query\": \"folder\" } } } }",
				versioning, lang, new PagingParametersImpl().setPerPage(1000L)));
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
				versioning, lang, new PagingParametersImpl().setPerPage(1000L)));
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
				versioning, lang));
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
				versioning, lang));
		assertThat(navigation.getChildren()).as("Navigation children").isNotNull();
		List<String> folderNames = navigation.getChildren().stream().map(item -> item.getNode().getDisplayName())
				.collect(Collectors.toList());
		assertFolderNames(folderNames, false);
	}

	@Test
	public void testChildrenInfo() {
		HibSchema folderSchema = tx(() -> schemaContainer("folder"));

		long expectedCount = StringUtils.equals(version, "published") ? 18L : 9L;

		if (StringUtils.equals(branch, BRANCH_NAME)) {
			// all pages again in the branch
			expectedCount = expectedCount * 2L;
		}

		NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, testFolder.getUuid(),
				versioning, lang));
		assertThat(response.getChildrenInfo()).as("Children Info").isNotNull().containsEntry("folder",
				new NodeChildrenInfo().setSchemaUuid(folderSchema.getUuid()).setCount(expectedCount));
	}

	/**
	 * Test getting nodes via graphQL
	 */
	@Test
	public void testGraphQLNodes() {
		doGraphQLTest("query ($en: String, $de: String) {nodes(lang: [$en, $de]) {elements {displayName }}}",
				"$.data.nodes.elements[*].displayName", true, "en", english ? "en" : null, "de", german ? "de" : null);
	}

	/**
	 * Test getting node children via graphql
	 */
	@Test
	public void testGraphQLChildren() {
		doGraphQLTest(
				"query ($uuid: String, $en: String, $de: String) { node(uuid: $uuid, lang: [$en, $de]) { uuid children(lang: [$en, $de]) { elements { displayName}}}}",
				"$.data.node.children.elements[*].displayName", false, "uuid", testFolder.getUuid(), "en",
				english ? "en" : null, "de", german ? "de" : null);
	}

	/**
	 * Test getting nodes of schema via graphql
	 */
	@Test
	public void testGraphQLNodesOfSchema() {
		doGraphQLTest(
				"query ($schema: String, $en: String, $de: String) { schema(name: $schema) { nodes(lang: [$en, $de]) { elements { displayName } } } }",
				"$.data.schema.nodes.elements[*].displayName", true, "schema", "folder", "en", english ? "en" : null,
				"de", german ? "de" : null);
	}

	/**
	 * Test getting nodes with tag via graphql
	 */
	@Test
	public void testGraphQLNodesWithTag() {
		doGraphQLTest("query ($en: String, $de: String) { tag(name:\"red\") { nodes(lang: [$en, $de]) { elements { displayName } } } }",
				"$.data.tag.nodes.elements[*].displayName", false, "en", english ? "en" : null,
						"de", german ? "de" : null);
	}

	/**
	 * Test searching for nodes via graphql
	 */
	@Test
	public void testGraphQLSearchNodes() {
		doGraphQLTest(
				"query ($search: String, $en: String, $de: String) { nodes(query: $search, lang: [$en, $de], perPage: 1000) { elements { displayName } } }",
				"$.data.nodes.elements[*].displayName", true, "search",
				"{ \"query\": { \"match\": { \"schema.name\": { \"query\": \"folder\" } } } }", "en",
				english ? "en" : null, "de", german ? "de" : null);
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
				() -> client().graphql(PROJECT_NAME, request, versioning));
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

		Set<String> expected = new HashSet<>();
		switch(version) {
		case "draft":
			if (english) {
				expected.addAll(Arrays.asList("New with read", "New with read en", "Modified Folder with read",
						"Modified Folder with read en", "Published with read", "Published with read en"));
			}
			if (german) {
				expected.addAll(Arrays.asList("Neu mit Lesen de", "Modifiziert Ordner mit Lesen de", "Publiziert mit Lesen de"));
				if (!english) {
					expected.addAll(Arrays.asList("Neu mit Lesen", "Modifiziert Ordner mit Lesen", "Publiziert mit Lesen"));
				}
			}
			break;
		case "published":
			if (english) {
				expected.addAll(Arrays.asList("Published", "Published with read", "Published en",
						"Published with read en", "Folder", "Folder en", "Folder with read", "Folder with read en"));
			}
			if (german) {
				expected.addAll(Arrays.asList("Publiziert de", "Publiziert mit Lesen de", "Ordner de", "Ordner mit Lesen de"));
				if (!english) {
					expected.addAll(Arrays.asList("Publiziert", "Publiziert mit Lesen", "Ordner", "Ordner mit Lesen"));
				}
			}
			break;
		default:
			fail("Unexpected version " + version);
		}

		if (StringUtils.equals(branch, BRANCH_NAME)) {
			switch(version) {
			case "draft":
				if (english) {
					expected.addAll(Arrays.asList("Alternative New with read", "Alternative New with read en", "Modified Alternative Folder with read",
							"Modified Alternative Folder with read en", "Alternative Published with read", "Alternative Published with read en"));
				}
				if (german) {
					expected.addAll(Arrays.asList("Alternativ Neu mit Lesen de", "Modifiziert Alternativ Ordner mit Lesen de", "Alternativ Publiziert mit Lesen de"));
					if (!english) {
						expected.addAll(Arrays.asList("Alternativ Neu mit Lesen", "Modifiziert Alternativ Ordner mit Lesen", "Alternativ Publiziert mit Lesen"));
					}
				}
				break;
			case "published":
				if (english) {
					expected.addAll(Arrays.asList("Alternative Published", "Alternative Published with read", "Alternative Published en",
							"Alternative Published with read en", "Alternative Folder", "Alternative Folder en", "Alternative Folder with read", "Alternative Folder with read en"));
				}
				if (german) {
					expected.addAll(Arrays.asList("Alternativ Publiziert de", "Alternativ Publiziert mit Lesen de", "Alternativ Ordner de", "Alternativ Ordner mit Lesen de"));
					if (!english) {
						expected.addAll(Arrays.asList("Alternativ Publiziert", "Alternativ Publiziert mit Lesen", "Alternativ Ordner", "Alternativ Ordner mit Lesen"));
					}
				}
				break;
			default:
				fail("Unexpected version " + version);
			}
		}

		if (includeOthers) {
			if (english) {
				expected.addAll(Arrays.asList("News", "2015", "Test Folder"));
			} else if (german) {
				expected.addAll(Arrays.asList(/* "Neuigkeiten", "2015", */ "Test Ordner"));
			}
		}

		assertThat(folderNames).as("Folder names").containsOnlyElementsOf(expected);

//		switch (version) {
//		case "draft":
//			if (includeOthers) {
//				assertThat(folderNames).as("Folder names").containsOnly(
//						"2015",
//						"News",
//						"Test Folder",
//						"New with read",
//						"Published with read",
//						"Modified with read");
//			} else {
//				assertThat(folderNames).as("Folder names").containsOnly(
//						"New with read",
//						"Published with read",
//						"Modified with read");
//			}
//			break;
//		case "published":
//			if (includeOthers) {
//				assertThat(folderNames).as("Folder names").containsOnly(
//						"2015",
//						"News",
//						"Test Folder",
//						"Published",
//						"Published with read",
//						"Original",
//						"Original with read");
//			} else {
//				assertThat(folderNames).as("Folder names").containsOnly(
//						"Published",
//						"Published with read",
//						"Original",
//						"Original with read");
//			}
//			break;
//		default:
//			fail("Unexpected version " + version);
//		}
	}

	/**
	 * Create a new folder
	 * @param parentNodeUuid parent node UUID
	 * @param nameEn folder name in english (will be the display name)
	 * @param nameDe folder name in german (will be the display name)
	 * @param setTag true to set the tag on the folder
	 * @param branch branch (may be null to create in the default branch)
	 * @return created folder
	 */
	protected NodeResponse createFolder(String parentNodeUuid, String nameEn, String nameDe, boolean setTag, String branch) {
		AtomicReference<String> nodeUuid = new AtomicReference<>();
		NodeResponse response = null;

		VersioningParameters param = new VersioningParametersImpl().setBranch(branch);

		if (!StringUtils.isEmpty(nameEn)) {
			NodeCreateRequest createRequest = new NodeCreateRequest();
			createRequest.setSchemaName("folder");
			createRequest.setParentNodeUuid(parentNodeUuid);
			createRequest.setLanguage("en");
			createRequest.getFields().put("name", FieldUtil.createStringField(nameEn));
			createRequest.getFields().put("slug", FieldUtil.createStringField(nameEn));
			response = call(() -> client().createNode(PROJECT_NAME, createRequest, param));
			nodeUuid.set(response.getUuid());
			if (setTag) {
				call(() -> client().addTagToNode(PROJECT_NAME, nodeUuid.get(), tag.getUuid(), param));
			}
		}
		if (!StringUtils.isEmpty(nameDe)) {
			NodeCreateRequest createRequest = new NodeCreateRequest();
			createRequest.setSchemaName("folder");
			createRequest.setParentNodeUuid(parentNodeUuid);
			createRequest.setLanguage("de");
			createRequest.getFields().put("name", FieldUtil.createStringField(nameDe));
			createRequest.getFields().put("slug", FieldUtil.createStringField(nameDe));
			if (!StringUtils.isEmpty(nodeUuid.get())) {
				response = call(() -> client().createNode(nodeUuid.get(), PROJECT_NAME, createRequest, param));
			} else {
				response = call(() -> client().createNode(PROJECT_NAME, createRequest, param));
			}
			nodeUuid.set(response.getUuid());
			if (setTag) {
				call(() -> client().addTagToNode(PROJECT_NAME, nodeUuid.get(), tag.getUuid(), param));
			}
		}

		return response;
	}

	/**
	 * Publish the given node
	 * @param node node to publish
	 * @param branch branch
	 * @return node
	 */
	protected NodeResponse publish(NodeResponse node, String branch) {
		VersioningParameters param = new VersioningParametersImpl().setBranch(branch);
		call(() -> client().publishNode(PROJECT_NAME, node.getUuid(), param));
		return node;
	}

	/**
	 * Update the given folder by setting the given new name
	 * @param node folder to update
	 * @param newNameEn new name in english
	 * @param newNameDe new name in german
	 * @param branch branch (may be null)
	 * @return updated node
	 */
	protected NodeResponse update(NodeResponse node, String newNameEn, String newNameDe, String branch) {
		VersioningParameters param = new VersioningParametersImpl().setBranch(branch);
		NodeResponse response = null;

		if (!StringUtils.isEmpty(newNameEn)) {
			NodeUpdateRequest updateRequest = new NodeUpdateRequest();
			updateRequest.setLanguage("en");
			updateRequest.setVersion("draft");
			updateRequest.getFields().put("name", FieldUtil.createStringField(newNameEn));
			response = call(() -> client().updateNode(PROJECT_NAME, node.getUuid(), updateRequest, param));
		}
		if (!StringUtils.isEmpty(newNameDe)) {
			NodeUpdateRequest updateRequest = new NodeUpdateRequest();
			updateRequest.setLanguage("de");
			updateRequest.setVersion("draft");
			updateRequest.getFields().put("name", FieldUtil.createStringField(newNameDe));
			response = call(() -> client().updateNode(PROJECT_NAME, node.getUuid(), updateRequest, param));
		}
		return response;
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
