package com.gentics.mesh.core.node;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.TestFullDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.context.MeshTestHelper.call;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.navigation.NavigationResponse;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.impl.NavigationParameters;
import com.gentics.mesh.parameter.impl.VersioningParameters;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.AbstractRestEndpointTest;

public class NodeNavigationEndpointTest extends AbstractRestEndpointTest {

	/**
	 * Test reading a node with a maxDepth value of zero
	 */
	@Test
	public void testReadChildrenDepthZero() {
		try (NoTx noTx = db.noTx()) {
			Node node = project().getBaseNode();
			String uuid = node.getUuid();
			assertNotNull(node);
			assertNotNull(node.getUuid());

			NavigationResponse response = call(() -> client().loadNavigation(PROJECT_NAME, uuid, new NavigationParameters().setMaxDepth(0),
					new VersioningParameters().draft()));
			assertEquals("The root uuid did not match the expected one.", uuid, response.getUuid());
			assertThat(response).hasDepth(0).isValid(1);
		}
	}

	/**
	 * Test fetching a navigation for a node which has no sub nodes.
	 */
	@Test
	public void testReadNodeWithNoChildren() {
		try (NoTx noTx = db.noTx()) {
			Node node = folder("2015");
			String uuid = node.getUuid();
			assertNotNull(node);
			assertNotNull(node.getUuid());

			NavigationResponse response = call(() -> client().loadNavigation(PROJECT_NAME, uuid, new NavigationParameters().setMaxDepth(42),
					new VersioningParameters().draft()));
			assertEquals("The root uuid did not match the expected one.", uuid, response.getUuid());
			assertThat(response).hasDepth(0).isValid(1);
		}
	}

	/**
	 * Test fetching a navigation with no request parameters.
	 */
	@Test
	public void testReadNavigationWithNoParameters() {
		try (NoTx noTx = db.noTx()) {
			Node node = project().getBaseNode();
			NavigationResponse response = call(() -> client().loadNavigation(PROJECT_NAME, node.getUuid(), new VersioningParameters().draft()));
			assertThat(response).hasDepth(3).isValid(7);
		}
	}

	/**
	 * Test fetching a navigation with a negative maxDepth parameter value.
	 */
	@Test
	public void testReadNavigationWithNegativeDepth() {
		try (NoTx noTx = db.noTx()) {
			Node node = folder("2015");
			call(() -> client().loadNavigation(PROJECT_NAME, node.getUuid(), new NavigationParameters().setMaxDepth(-10),
					new VersioningParameters().draft()), BAD_REQUEST, "navigation_error_invalid_max_depth");
		}
	}

	/**
	 * Test fetching a navigation for a node which is not a container node.
	 */
	@Test
	public void testReadNoContainerNode() {
		try (NoTx noTx = db.noTx()) {
			Node node = content();
			assertFalse("The node must not be a container.", node.getSchemaContainer().getLatestVersion().getSchema().isContainer());
			call(() -> client().loadNavigation(PROJECT_NAME, node.getUuid(), new NavigationParameters().setMaxDepth(1),
					new VersioningParameters().draft()), BAD_REQUEST, "navigation_error_no_container");
		}
	}

	/**
	 * Test reading a node with a maxDepth value of one.
	 */
	@Test
	public void testReadChildrenDepthOne() {
		try (NoTx noTx = db.noTx()) {
			Node node = project().getBaseNode();
			String uuid = node.getUuid();
			assertNotNull(node);
			assertNotNull(node.getUuid());

			NavigationResponse response = call(() -> client().loadNavigation(PROJECT_NAME, uuid, new NavigationParameters().setMaxDepth(1),
					new VersioningParameters().draft()));

			assertThat(response).hasDepth(1).isValid(4);
			assertEquals("The root uuid did not match the expected one.", uuid, response.getUuid());
		}
	}

	/**
	 * Test reading a node with a maxDepth value of two.
	 */
	@Test
	public void testReadChildrenDepthTwo() {
		try (NoTx noTx = db.noTx()) {
			Node node = project().getBaseNode();
			String uuid = node.getUuid();
			assertNotNull(node);
			assertNotNull(node.getUuid());

			NavigationResponse response = call(() -> client().loadNavigation(PROJECT_NAME, uuid, new NavigationParameters().setMaxDepth(2),
					new VersioningParameters().draft()));
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
		try (NoTx noTx = db.noTx()) {
			Node node = folder("news");
			String uuid = node.getUuid();
			assertNotNull(node);
			assertNotNull(node.getUuid());

			MeshResponse<NavigationResponse> future = client()
					.loadNavigation(PROJECT_NAME, uuid, new NavigationParameters().setMaxDepth(2).setIncludeAll(true)).invoke();
			latchFor(future);
			assertSuccess(future);
			NavigationResponse response = future.result();
			assertEquals("The root uuid did not match the expected one.", uuid, response.getUuid());

			String[] expectedNodes = { "2015", "2014", "News Overview_english_name" };
			List<String> nodeNames = response.getChildren().stream().map(e -> e.getNode().getFields().getStringField("name").getString())
					.collect(Collectors.toList());
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
		try (NoTx noTx = db.noTx()) {
			Node node = folder("news");
			String uuid = node.getUuid();
			assertNotNull(node);
			assertNotNull(node.getUuid());

			MeshResponse<NavigationResponse> future = client()
					.loadNavigation(PROJECT_NAME, uuid, new NavigationParameters().setMaxDepth(2).setIncludeAll(false)).invoke();
			latchFor(future);
			assertSuccess(future);
			NavigationResponse response = future.result();
			assertEquals("The root uuid did not match the expected one.", uuid, response.getUuid());

			String[] expectedNodes = { "2015", "2014" };
			List<String> nodeNames = response.getChildren().stream().map(e -> e.getNode().getFields().getStringField("name").getString())
					.collect(Collectors.toList());
			assertThat(response).hasDepth(2).isValid(4);
			assertThat(nodeNames).containsExactly(expectedNodes);
		}
	}

	/**
	 * Test reading a node with a very high maxDepth parameter value which would exceed the actual depth of the returned tree.
	 */
	@Test
	public void testReadChildrenHighDepth() {
		try (NoTx noTx = db.noTx()) {
			Node node = project().getBaseNode();
			String uuid = node.getUuid();
			assertNotNull(node);
			assertNotNull(node.getUuid());

			NavigationResponse response = call(() -> client().loadNavigation(PROJECT_NAME, uuid, new NavigationParameters().setMaxDepth(42),
					new VersioningParameters().draft()));
			assertEquals(uuid, response.getUuid());
			assertNotNull("root was null", response);
		}
	}

	@Test
	public void testNavigationForRelease() {
		try (NoTx noTx = db.noTx()) {
			Project project = project();
			Node baseNode = project.getBaseNode();
			String baseNodeUuid = baseNode.getUuid();

			// latest release
			NavigationResponse response = call(() -> client().loadNavigation(PROJECT_NAME, baseNodeUuid, new NavigationParameters().setMaxDepth(1),
					new VersioningParameters().draft()));
			assertThat(response).hasDepth(1).isValid(4);

			Release initialRelease = project.getInitialRelease();
			Release newRelease = project.getReleaseRoot().create("newrelease", user());

			// latest release (again)
			response = call(() -> client().loadNavigation(PROJECT_NAME, baseNodeUuid, new NavigationParameters().setMaxDepth(1),
					new VersioningParameters().draft()));
			assertThat(response).hasDepth(0);

			// latest release by name
			response = call(() -> client().loadNavigation(PROJECT_NAME, baseNodeUuid, new NavigationParameters().setMaxDepth(1),
					new VersioningParameters().draft().setRelease(newRelease.getName())));
			assertThat(response).hasDepth(0);

			// initial release by name
			response = call(() -> client().loadNavigation(PROJECT_NAME, baseNodeUuid, new NavigationParameters().setMaxDepth(1),
					new VersioningParameters().draft().setRelease(initialRelease.getName())));
			assertThat(response).hasDepth(1).isValid(4);
		}
	}

	@Test
	public void testPublishedNavigation() {
		// TODO
	}

	@Test
	public void testPublishedNavigationForRelease() {
		// TODO
	}
}
