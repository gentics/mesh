package com.gentics.mesh.core.node;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.navigation.NavigationResponse;
import com.gentics.mesh.core.verticle.node.NodeVerticle;
import com.gentics.mesh.query.impl.NavigationRequestParameter;
import com.gentics.mesh.test.AbstractRestVerticleTest;

import io.vertx.core.Future;

public class NodeNavigationVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private NodeVerticle verticle;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(verticle);
		return list;
	}

	/**
	 * Test reading a node with a maxDepth value of zero
	 */
	@Test
	public void testReadChildrenDepthZero() {
		getClient().getClientSchemaStorage().addSchema(schemaContainer("folder").getSchema());

		Node node = project().getBaseNode();
		String uuid = node.getUuid();
		assertNotNull(node);
		assertNotNull(node.getUuid());

		Future<NavigationResponse> future = getClient().loadNavigation(PROJECT_NAME, uuid, new NavigationRequestParameter().setMaxDepth(0));
		latchFor(future);
		assertSuccess(future);
		NavigationResponse response = future.result();
		assertEquals("The root uuid did not match the expected one.", uuid, response.getRoot().getUuid());
		assertThat(response).hasDepth(0).isValid(1);
		assertNull("There should be no child elements for the root element", response.getRoot().getChildren());
	}

	/**
	 * Test fetching a navigation for a node which has no sub nodes.
	 */
	@Test
	public void testReadNodeWithNoChildren() {
		getClient().getClientSchemaStorage().addSchema(schemaContainer("folder").getSchema());

		Node node = folder("2015");
		String uuid = node.getUuid();
		assertNotNull(node);
		assertNotNull(node.getUuid());

		Future<NavigationResponse> future = getClient().loadNavigation(PROJECT_NAME, uuid, new NavigationRequestParameter().setMaxDepth(42));
		latchFor(future);
		assertSuccess(future);
		NavigationResponse response = future.result();
		assertEquals("The root uuid did not match the expected one.", uuid, response.getRoot().getUuid());
		assertNull("There should be no child elements for the root element", response.getRoot().getChildren());
		assertThat(response).hasDepth(0).isValid(1);
	}

	/**
	 * Test fetching a navigation with no request parameters.
	 */
	@Test
	public void testReadNavigationWithNoParameters() {
		Node node = project().getBaseNode();
		Future<NavigationResponse> future = getClient().loadNavigation(PROJECT_NAME, node.getUuid());
		latchFor(future);
		assertSuccess(future);
		NavigationResponse response = future.result();
		assertThat(response).hasDepth(3).isValid(7);
	}

	/**
	 * Test fetching a navigation with a negative maxDepth parameter value.
	 */
	@Test
	public void testReadNavigationWithNegativeDepth() {
		Node node = folder("2015");
		Future<NavigationResponse> future = getClient().loadNavigation(PROJECT_NAME, node.getUuid(),
				new NavigationRequestParameter().setMaxDepth(-10));
		latchFor(future);
		expectException(future, BAD_REQUEST, "navigation_error_invalid_max_depth");
	}

	/**
	 * Test fetching a navigation for a node which is not a container node.
	 */
	@Test
	public void testReadNoContainerNode() {

		Node node = content();

		assertFalse("The node must not be a container.", node.getSchema().isContainer());
		Future<NavigationResponse> future = getClient().loadNavigation(PROJECT_NAME, node.getUuid(), new NavigationRequestParameter().setMaxDepth(1));
		latchFor(future);
		expectException(future, BAD_REQUEST, "navigation_error_no_container");
	}

	/**
	 * Test reading a node with a maxDepth value of one.
	 */
	@Test
	public void testReadChildrenDepthOne() {

		getClient().getClientSchemaStorage().addSchema(schemaContainer("folder").getSchema());

		Node node = project().getBaseNode();
		String uuid = node.getUuid();
		assertNotNull(node);
		assertNotNull(node.getUuid());

		Future<NavigationResponse> future = getClient().loadNavigation(PROJECT_NAME, uuid, new NavigationRequestParameter().setMaxDepth(1));
		latchFor(future);
		assertSuccess(future);
		NavigationResponse response = future.result();

		assertThat(response).hasDepth(1).isValid(4);
		assertEquals("The root uuid did not match the expected one.", uuid, response.getRoot().getUuid());
		
	}

	/**
	 * Test reading a node with a maxDepth value of two.
	 */
	@Test
	public void testReadChildrenDepthTwo() {
		getClient().getClientSchemaStorage().addSchema(schemaContainer("folder").getSchema());

		Node node = project().getBaseNode();
		String uuid = node.getUuid();
		assertNotNull(node);
		assertNotNull(node.getUuid());

		Future<NavigationResponse> future = getClient().loadNavigation(PROJECT_NAME, uuid, new NavigationRequestParameter().setMaxDepth(2));
		latchFor(future);
		assertSuccess(future);
		NavigationResponse response = future.result();
		assertEquals("The root uuid did not match the expected one.", uuid, response.getRoot().getUuid());
		assertThat(response).hasDepth(2).isValid(6);
	}

	/**
	 * Test reading a node with a very high maxDepth parameter value which would exceed the actual depth of the returned tree.
	 */
	@Test
	public void testReadChildrenHighDepth() {
		getClient().getClientSchemaStorage().addSchema(schemaContainer("folder").getSchema());

		Node node = project().getBaseNode();
		String uuid = node.getUuid();
		assertNotNull(node);
		assertNotNull(node.getUuid());

		Future<NavigationResponse> future = getClient().loadNavigation(PROJECT_NAME, uuid, new NavigationRequestParameter().setMaxDepth(42));
		latchFor(future);
		assertSuccess(future);
		NavigationResponse response = future.result();
		//		assertNotNull("nodes field was null", response.getNodes());
		assertEquals(uuid, response.getRoot().getUuid());
		//		assertEquals(uuid, response.getNodes().get(uuid).getUuid());
		assertNotNull("root was null", response.getRoot());
	}



}