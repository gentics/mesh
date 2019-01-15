package com.gentics.mesh.core.node;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.INITIAL_BRANCH_NAME;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.navigation.NavigationElement;
import com.gentics.mesh.core.rest.navigation.NavigationResponse;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.client.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.NavigationParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.syncleus.ferma.tx.Tx;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class NodeNavigationEndpointTest extends AbstractMeshTest {

	/**
	 * Test reading a node with a maxDepth value of zero
	 */
	@Test
	public void testReadChildrenDepthZero() {
		try (Tx tx = tx()) {
			Node node = project().getBaseNode();
			String uuid = node.getUuid();
			assertNotNull(node);
			assertNotNull(node.getUuid());

			NavigationResponse response = call(() -> client().loadNavigation(PROJECT_NAME, uuid, new NavigationParametersImpl().setMaxDepth(0),
					new VersioningParametersImpl().draft()));
			assertEquals("The root uuid did not match the expected one.", uuid, response.getUuid());
			assertThat(response).hasDepth(0).isValid(1);
		}
	}

	/**
	 * Test fetching a navigation for a node which has no sub nodes.
	 */
	@Test
	public void testReadNodeWithNoChildren() {
		try (Tx tx = tx()) {
			Node node = folder("2015");
			String uuid = node.getUuid();
			assertNotNull(node);
			assertNotNull(node.getUuid());

			NavigationResponse response = call(() -> client().loadNavigation(PROJECT_NAME, uuid, new NavigationParametersImpl().setMaxDepth(42),
					new VersioningParametersImpl().draft()));
			assertEquals("The root uuid did not match the expected one.", uuid, response.getUuid());
			assertThat(response).hasDepth(0).isValid(1);
		}
	}

	/**
	 * Test fetching a navigation with no request parameters.
	 */
	@Test
	public void testReadNavigationWithNoParameters() {
		try (Tx tx = tx()) {
			Node node = project().getBaseNode();
			NavigationResponse response = call(() -> client().loadNavigation(PROJECT_NAME, node.getUuid(), new VersioningParametersImpl().draft()));
			assertThat(response).hasDepth(3).isValid(7);
		}
	}

	/**
	 * Test fetching a navigation with a negative maxDepth parameter value.
	 */
	@Test
	public void testReadNavigationWithNegativeDepth() {
		try (Tx tx = tx()) {
			Node node = folder("2015");
			call(() -> client().loadNavigation(PROJECT_NAME, node.getUuid(), new NavigationParametersImpl().setMaxDepth(-10),
					new VersioningParametersImpl().draft()), BAD_REQUEST, "navigation_error_invalid_max_depth");
		}
	}

	/**
	 * Test fetching a navigation for a node which is not a container node.
	 */
	@Test
	public void testReadNoContainerNode() {
		try (Tx tx = tx()) {
			Node node = content();
			assertFalse("The node must not be a container.", node.getSchemaContainer().getLatestVersion().getSchema().isContainer());
			call(() -> client().loadNavigation(PROJECT_NAME, node.getUuid(), new NavigationParametersImpl().setMaxDepth(1),
					new VersioningParametersImpl().draft()), BAD_REQUEST, "navigation_error_no_container");
		}
	}

	/**
	 * Test reading a node with a maxDepth value of one.
	 */
	@Test
	public void testReadChildrenDepthOne() {
		try (Tx tx = tx()) {
			Node node = project().getBaseNode();
			String uuid = node.getUuid();
			assertNotNull(node);
			assertNotNull(node.getUuid());

			NavigationResponse response = call(() -> client().loadNavigation(PROJECT_NAME, uuid, new NavigationParametersImpl().setMaxDepth(1),
					new VersioningParametersImpl().draft()));

			assertThat(response).hasDepth(1).isValid(4);
			assertEquals("The root uuid did not match the expected one.", uuid, response.getUuid());
		}
	}

	/**
	 * Test reading a node with a maxDepth value of two.
	 */
	@Test
	public void testReadChildrenDepthTwo() {
		try (Tx tx = tx()) {
			Node node = project().getBaseNode();
			String uuid = node.getUuid();
			assertNotNull(node);
			assertNotNull(node.getUuid());

			NavigationResponse response = call(() -> client().loadNavigation(PROJECT_NAME, uuid, new NavigationParametersImpl().setMaxDepth(2),
					new VersioningParametersImpl().draft()));
			assertEquals("The root uuid did not match the expected one.", uuid, response.getUuid());
			assertThat(response).hasDepth(2).isValid(6);
		}
	}

	/**
	 * Test reading a node with a maxDepth value of two and the includeAll flag set to true.
	 * 
	 * We expect the response to also include regular content nodes.
	 */
	@Test
	public void testReadChildrenDepthTwoIncludeAll() {
		try (Tx tx = tx()) {
			Node node = folder("news");
			String uuid = node.getUuid();
			assertNotNull(node);
			assertNotNull(node.getUuid());

			NavigationResponse response = client()
				.loadNavigation(PROJECT_NAME, uuid, new NavigationParametersImpl().setMaxDepth(2).setIncludeAll(true)).blockingGet();
			assertEquals("The root uuid did not match the expected one.", uuid, response.getUuid());

			String[] expectedNodes = {"2015", "2014", "News Overview_english_name"};
			List<String> nodeNames = response.getChildren().stream().map(e -> {
				StringField titleField = e.getNode().getFields().getStringField("teaser");
				StringField slugField = e.getNode().getFields().getStringField("slug");
				if (titleField != null) {
					return titleField.getString();
				} else {
					return slugField.getString();
				}
			}).collect(toList());
			assertThat(response).hasDepth(2).isValid(8);
			assertThat(nodeNames).containsExactly(expectedNodes);
		}
	}

	/**
	 * Test reading a node with a maxDepth value of two and the includeAll flag set to false.
	 * 
	 * We expect the response to only include container nodes.
	 */
	@Test
	public void testReadChildrenDepthTwoIncludeAllDisabled() {
		try (Tx tx = tx()) {
			Node node = folder("news");
			String uuid = node.getUuid();
			assertNotNull(node);
			assertNotNull(node.getUuid());

			NavigationResponse response = client()
				.loadNavigation(PROJECT_NAME, uuid, new NavigationParametersImpl().setMaxDepth(2).setIncludeAll(false)).blockingGet();
			assertEquals("The root uuid did not match the expected one.", uuid, response.getUuid());

			String[] expectedNodes = {"2015", "2014"};
			List<String> nodeNames = response.getChildren().stream().map(e -> e.getNode().getFields().getStringField("name").getString())
				.collect(toList());
			assertThat(response).hasDepth(2).isValid(4);
			assertThat(nodeNames).containsExactly(expectedNodes);
		}
	}

	/**
	 * Test reading a node with a very high maxDepth parameter value which would exceed the actual depth of the returned tree.
	 */
	@Test
	public void testReadChildrenHighDepth() {
		String uuid = db().tx(() -> project().getBaseNode().getUuid());

		NavigationResponse response = call(() -> client().loadNavigation(PROJECT_NAME, uuid, new NavigationParametersImpl().setMaxDepth(42)));
		assertEquals(uuid, response.getUuid());
		assertNotNull("root was null", response);
	}

	@Test
	public void testNavigationLanguageFallback() {
		String baseNodeUuid = db().tx(() -> project().getBaseNode().getUuid());

		// level 0
		NodeCreateRequest request = new NodeCreateRequest();
		request.setSchema(new SchemaReferenceImpl().setName("folder"));
		request.setLanguage("en");
		request.getFields().put("name", FieldUtil.createStringField("english folder-0"));
		request.getFields().put("slug", FieldUtil.createStringField("english folder-0"));
		request.setParentNodeUuid(baseNodeUuid);
		NodeResponse englishFolder0 = call(() -> client().createNode(PROJECT_NAME, request));

		// level 1
		request.setParentNodeUuid(englishFolder0.getUuid());
		request.getFields().put("name", FieldUtil.createStringField("english folder-1"));
		request.getFields().put("slug", FieldUtil.createStringField("english folder-1"));
		NodeResponse englishFolder1 = call(() -> client().createNode(PROJECT_NAME, request));

		// level 2
		request.setLanguage("de");
		request.setParentNodeUuid(englishFolder1.getUuid());
		request.getFields().put("name", FieldUtil.createStringField("german folder-2"));
		request.getFields().put("slug", FieldUtil.createStringField("german folder-2"));
		NodeResponse germanFolderResponse = call(() -> client().createNode(PROJECT_NAME, request));

		NavigationResponse navResponse = call(() -> client().loadNavigation(PROJECT_NAME, baseNodeUuid,
				new NavigationParametersImpl().setMaxDepth(42), new NodeParametersImpl().setLanguages("de").setResolveLinks(LinkType.FULL)));

		assertEquals("/api/v1/dummy/webroot/english%20folder-0/english%20folder-1/german%20folder-2",
				findFolder(navResponse, germanFolderResponse.getUuid()).getPath());
		assertEquals("/api/v1/dummy/webroot/english%20folder-0/english%20folder-1", findFolder(navResponse, englishFolder1.getUuid()).getPath());
		assertEquals("/api/v1/dummy/webroot/english%20folder-0", findFolder(navResponse, englishFolder0.getUuid()).getPath());

	}

	private NodeResponse findFolder(NavigationResponse nav, String uuid) {
		return findFolder(nav.getChildren(), uuid);
	}

	/**
	 * Recurse through the whole navigation list and try to locate the node with the given uuid.
	 * 
	 * @param navList
	 * @param uuid
	 * @return
	 */
	private NodeResponse findFolder(List<NavigationElement> navList, String uuid) {
		for (NavigationElement element : navList) {
			if (element.getUuid().equals(uuid)) {
				return element.getNode();
			}
			if (element.getChildren() != null) {
				NodeResponse result = findFolder(element.getChildren(), uuid);
				if (result != null) {
					return result;
				}
			}
		}
		return null;
	}

	@Test
	public void testNavigationForBranch() {
		Project project = project();
		String newBranchName = "newbranch";
		String baseNodeUuid = tx(() -> project.getBaseNode().getUuid());

		// latest branch
		NavigationResponse response = call(() -> client().loadNavigation(PROJECT_NAME, baseNodeUuid, new NavigationParametersImpl().setMaxDepth(1),
				new VersioningParametersImpl().draft()));
		assertThat(response).hasDepth(1).isValid(4);

		try (Tx tx = tx()) {
			project.getBranchRoot().create(newBranchName, user());
			tx.success();
		}

		// latest branch (again)
		response = call(() -> client().loadNavigation(PROJECT_NAME, baseNodeUuid, new NavigationParametersImpl().setMaxDepth(1),
				new VersioningParametersImpl().draft()));
		assertThat(response).hasDepth(0);

		// latest branch by name
		response = call(() -> client().loadNavigation(PROJECT_NAME, baseNodeUuid, new NavigationParametersImpl().setMaxDepth(1),
				new VersioningParametersImpl().draft().setBranch(newBranchName)));
		assertThat(response).hasDepth(0);

		// initial branch by name
		response = call(() -> client().loadNavigation(PROJECT_NAME, baseNodeUuid, new NavigationParametersImpl().setMaxDepth(1),
				new VersioningParametersImpl().draft().setBranch(INITIAL_BRANCH_NAME)));
		assertThat(response).hasDepth(1).isValid(4);
	}

	@Test
	public void testPublishedNavigation() {
		// TODO
	}

	@Test
	public void testPublishedNavigationForBranch() {
		// TODO
	}
}
