package com.gentics.mesh.core.node;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.mock.Mocks.getMockedInternalActionContext;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.assertElement;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.failingLatch;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.VersionReference;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.demo.UserInfo;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.impl.LinkType;
import com.gentics.mesh.parameter.impl.NodeParameters;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.PublishParameters;
import com.gentics.mesh.parameter.impl.RolePermissionParameters;
import com.gentics.mesh.parameter.impl.VersioningParameters;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.AbstractBasicCrudEndpointTest;
import com.gentics.mesh.util.VersionNumber;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class NodeEndpointTest extends AbstractBasicCrudEndpointTest {

	// Create tests

	@Test
	public void testCreateNodeWithNoLanguageCode() {
		try (NoTx noTx = db.noTx()) {
			NodeCreateRequest request = new NodeCreateRequest();
			SchemaReference schemaReference = new SchemaReference();
			schemaReference.setName("content");
			schemaReference.setUuid(schemaContainer("content").getUuid());
			// No language code set
			request.getFields().put("name", FieldUtil.createStringField("some name"));
			request.getFields().put("filename", FieldUtil.createStringField("new-page.html"));
			request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
			request.setSchema(schemaReference);
			request.setParentNodeUuid(folder("news").getUuid());

			assertThat(dummySearchProvider).recordedStoreEvents(0);
			MeshResponse<NodeResponse> future = client().createNode(PROJECT_NAME, request).invoke();
			latchFor(future);
			expectException(future, BAD_REQUEST, "node_no_languagecode_specified");
			assertThat(dummySearchProvider).recordedStoreEvents(0);
		}
	}

	@Test
	public void testCreateNodeWithBogusLanguageCode() throws GenericRestException, Exception {
		try (NoTx noTx = db.noTx()) {
			NodeCreateRequest request = new NodeCreateRequest();
			SchemaReference schemaReference = new SchemaReference();
			schemaReference.setName("content");
			schemaReference.setUuid(schemaContainer("content").getUuid());
			schemaReference.setVersion(1);
			request.setLanguage("BOGUS");
			request.getFields().put("name", FieldUtil.createStringField("some name"));
			request.getFields().put("filename", FieldUtil.createStringField("new-page.html"));
			request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
			request.setSchema(schemaReference);
			request.setParentNodeUuid(folder("news").getUuid());

			assertThat(dummySearchProvider).recordedStoreEvents(0);
			MeshResponse<NodeResponse> future = client().createNode(PROJECT_NAME, request).invoke();
			latchFor(future);
			expectException(future, BAD_REQUEST, "language_not_found", "BOGUS");
			assertThat(dummySearchProvider).recordedStoreEvents(0);
		}
	}

	@Test
	public void testCreateNodeInBaseNode() {
		try (NoTx noTx = db.noTx()) {
			NodeCreateRequest request = new NodeCreateRequest();
			request.setSchema(new SchemaReference().setVersion(1).setName("content").setUuid(schemaContainer("content").getUuid()));
			request.setLanguage("en");
			request.getFields().put("title", FieldUtil.createStringField("some title"));
			request.getFields().put("name", FieldUtil.createStringField("some name"));
			request.getFields().put("filename", FieldUtil.createStringField("new-page.html"));
			request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
			request.setParentNodeUuid(project().getBaseNode().getUuid());

			assertThat(dummySearchProvider).recordedStoreEvents(0);
			NodeResponse restNode = call(() -> client().createNode(PROJECT_NAME, request));
			assertThat(restNode).matches(request);
			assertThat(dummySearchProvider).recordedStoreEvents(1);
		}
	}

	@Test
	public void testCreateFolder() {
		try (NoTx noTx = db.noTx()) {
			Node parentNode = folder("news");
			String uuid = parentNode.getUuid();
			assertNotNull(parentNode);
			assertNotNull(parentNode.getUuid());

			NodeCreateRequest request = new NodeCreateRequest();
			request.setSchema(new SchemaReference().setName("folder").setVersion(1).setUuid(schemaContainer("folder").getUuid()));
			request.setLanguage("en");
			request.getFields().put("name", FieldUtil.createStringField("some name"));
			request.setParentNodeUuid(uuid);

			assertThat(dummySearchProvider).recordedStoreEvents(0);
			NodeResponse restNode = call(() -> client().createNode(PROJECT_NAME, request));
			assertThat(restNode).matches(request);
			assertThat(dummySearchProvider).recordedStoreEvents(1);
		}
	}

	@Test
	public void testCreateMultiple() {
		// TODO migrate test to performance tests
		try (NoTx noTx = db.noTx()) {
			Node parentNode = folder("news");
			String uuid = parentNode.getUuid();
			assertNotNull(parentNode);
			assertNotNull(parentNode.getUuid());

			long start = System.currentTimeMillis();
			for (int i = 1; i < 100; i++) {
				dummySearchProvider.reset();
				NodeCreateRequest request = new NodeCreateRequest();
				request.setSchema(new SchemaReference().setName("content").setUuid(schemaContainer("content").getUuid()));
				request.setLanguage("en");
				request.getFields().put("title", FieldUtil.createStringField("some title " + i));
				request.getFields().put("name", FieldUtil.createStringField("some name " + i));
				request.getFields().put("filename", FieldUtil.createStringField("new-page_" + i + ".html"));
				request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
				request.setParentNodeUuid(uuid);

				MeshResponse<NodeResponse> future = client().createNode(PROJECT_NAME, request).invoke();
				latchFor(future);
				assertSuccess(future);
				long duration = System.currentTimeMillis() - start;
				System.out.println("Duration:" + i + " " + (duration / i));
			}
		}
	}

	@Test
	@Override
	public void testCreate() throws Exception {

		String parentNodeUuid = db.noTx(() -> folder("news").getUuid());
		NodeCreateRequest request = new NodeCreateRequest();
		request.setSchema(new SchemaReference().setName("content"));
		request.setLanguage("en");
		request.getFields().put("title", FieldUtil.createStringField("some title"));
		request.getFields().put("name", FieldUtil.createStringField("some name"));
		request.getFields().put("filename", FieldUtil.createStringField("new-page.html"));
		request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
		request.setParentNodeUuid(parentNodeUuid);

		try (NoTx noTx = db.noTx()) {

			assertThat(dummySearchProvider).recordedStoreEvents(0);
			NodeResponse restNode = call(() -> client().createNode(PROJECT_NAME, request));
			assertThat(restNode).matches(request);
			assertThat(dummySearchProvider).recordedStoreEvents(1);
		}
	}

	@Override
	public void testCreateWithNoPerm() throws Exception {

		String parentNodeUuid = db.noTx(() -> folder("news").getUuid());

		NodeCreateRequest request = new NodeCreateRequest();
		request.setSchema(new SchemaReference().setName("content"));
		request.setLanguage("en");
		request.getFields().put("title", FieldUtil.createStringField("some title"));
		request.getFields().put("name", FieldUtil.createStringField("some name"));
		request.getFields().put("filename", FieldUtil.createStringField("new-page.html"));
		request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
		request.setParentNodeUuid(parentNodeUuid);

		try (NoTx noTx = db.noTx()) {
			role().revokePermissions(meshRoot().getNodeRoot(), CREATE_PERM);
		}

		call(() -> client().createNode(PROJECT_NAME, request), FORBIDDEN, "error_missing_perm");

	}

	@Test
	public void testCreateForReleaseByName() {
		try (NoTx noTx = db.noTx()) {
			Project project = project();
			Release initialRelease = project.getReleaseRoot().getInitialRelease();
			Release newRelease = project.getReleaseRoot().create("newrelease", user());

			Node parentNode = folder("news");
			String uuid = parentNode.getUuid();
			NodeCreateRequest request = new NodeCreateRequest();
			request.setSchema(new SchemaReference().setName("content").setUuid(schemaContainer("content").getUuid()));
			request.setLanguage("en");
			request.getFields().put("title", FieldUtil.createStringField("some title"));
			request.getFields().put("name", FieldUtil.createStringField("some name"));
			request.getFields().put("filename", FieldUtil.createStringField("new-page.html"));
			request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
			request.setParentNodeUuid(uuid);

			NodeResponse nodeResponse = call(
					() -> client().createNode(project.getName(), request, new VersioningParameters().setRelease(initialRelease.getName())));

			meshRoot().getNodeRoot().reload();
			Node newNode = meshRoot().getNodeRoot().findByUuid(nodeResponse.getUuid());
			for (ContainerType type : Arrays.asList(ContainerType.INITIAL, ContainerType.DRAFT)) {
				assertThat(newNode.getGraphFieldContainer("en", initialRelease.getUuid(), type)).as(type + " Field container for initial release")
						.isNotNull().hasVersion("0.1");
				assertThat(newNode.getGraphFieldContainer("en", newRelease.getUuid(), type)).as(type + " Field Container for new release").isNull();
			}
		}
	}

	@Test
	public void testCreateForReleaseByUuid() {
		try (NoTx noTx = db.noTx()) {
			Project project = project();
			Release initialRelease = project.getReleaseRoot().getInitialRelease();
			Release newRelease = project.getReleaseRoot().create("newrelease", user());

			Node parentNode = folder("news");
			String uuid = parentNode.getUuid();
			NodeCreateRequest request = new NodeCreateRequest();
			request.setSchema(new SchemaReference().setName("content").setUuid(schemaContainer("content").getUuid()));
			request.setLanguage("en");
			request.getFields().put("title", FieldUtil.createStringField("some title"));
			request.getFields().put("name", FieldUtil.createStringField("some name"));
			request.getFields().put("filename", FieldUtil.createStringField("new-page.html"));
			request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
			request.setParentNodeUuid(uuid);

			NodeResponse nodeResponse = call(
					() -> client().createNode(project.getName(), request, new VersioningParameters().setRelease(initialRelease.getUuid())));

			meshRoot().getNodeRoot().reload();
			Node newNode = meshRoot().getNodeRoot().findByUuid(nodeResponse.getUuid());
			for (ContainerType type : Arrays.asList(ContainerType.INITIAL, ContainerType.DRAFT)) {
				assertThat(newNode.getGraphFieldContainer("en", initialRelease.getUuid(), type)).as(type + " Field container for initial release")
						.isNotNull().hasVersion("0.1");
				assertThat(newNode.getGraphFieldContainer("en", newRelease.getUuid(), type)).as(type + " Field Container for new release").isNull();
			}
		}
	}

	@Test
	public void testCreateForLatestRelease() {
		try (NoTx noTx = db.noTx()) {
			Project project = project();
			Release initialRelease = project.getReleaseRoot().getInitialRelease();
			Release newRelease = project.getReleaseRoot().create("newrelease", user());

			Node parentNode = folder("news");
			String uuid = parentNode.getUuid();
			NodeCreateRequest request = new NodeCreateRequest();
			request.setSchema(new SchemaReference().setName("content").setUuid(schemaContainer("content").getUuid()));
			request.setLanguage("en");
			request.getFields().put("title", FieldUtil.createStringField("some title"));
			request.getFields().put("name", FieldUtil.createStringField("some name"));
			request.getFields().put("filename", FieldUtil.createStringField("new-page.html"));
			request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
			request.setParentNodeUuid(uuid);

			NodeResponse nodeResponse = call(() -> client().createNode(project.getName(), request));

			meshRoot().getNodeRoot().reload();
			Node newNode = meshRoot().getNodeRoot().findByUuid(nodeResponse.getUuid());

			for (ContainerType type : Arrays.asList(ContainerType.INITIAL, ContainerType.DRAFT)) {
				assertThat(newNode.getGraphFieldContainer("en", initialRelease.getUuid(), type)).as(type + " Field container for initial release")
						.isNull();
				assertThat(newNode.getGraphFieldContainer("en", newRelease.getUuid(), type)).as(type + " Field Container for new release").isNotNull()
						.hasVersion("0.1");
			}
		}
	}

	@Test
	public void testCreateForBogusRelease() {
		try (NoTx noTx = db.noTx()) {
			Project project = project();
			project.getReleaseRoot().create("newrelease", user());

			Node parentNode = folder("news");
			String uuid = parentNode.getUuid();
			NodeCreateRequest request = new NodeCreateRequest();
			request.setSchema(new SchemaReference().setName("content").setUuid(schemaContainer("content").getUuid()));
			request.setLanguage("en");
			request.getFields().put("title", FieldUtil.createStringField("some title"));
			request.getFields().put("name", FieldUtil.createStringField("some name"));
			request.getFields().put("filename", FieldUtil.createStringField("new-page.html"));
			request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
			request.setParentNodeUuid(uuid);

			call(() -> client().createNode(project.getName(), request, new VersioningParameters().setRelease("bogusrelease")), BAD_REQUEST,
					"release_error_not_found", "bogusrelease");
		}
	}

	@Test
	@Ignore
	public void testCreateNodeAndCheckReadOnlyRole() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testCreateReadDelete() throws Exception {
		try (NoTx noTx = db.noTx()) {
			NodeResponse restNode2;

			NodeCreateRequest request = new NodeCreateRequest();
			SchemaReference schemaReference = new SchemaReference();
			schemaReference.setName("content");
			schemaReference.setUuid(schemaContainer("content").getUuid());
			request.setSchema(schemaReference);

			request.setLanguage("de");
			request.getFields().put("name", FieldUtil.createStringField("some name"));
			request.getFields().put("title", FieldUtil.createStringField("Title"));
			request.getFields().put("filename", FieldUtil.createStringField("new-page.html"));
			request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
			request.setParentNodeUuid(folder("news").getUuid());

			assertThat(dummySearchProvider).recordedStoreEvents(0);
			NodeResponse restNode = call(
					() -> client().createNode(PROJECT_NAME, request, new NodeParameters().setLanguages("de"), new VersioningParameters().draft()));
			assertThat(dummySearchProvider).recordedStoreEvents(1);
			assertThat(restNode).matches(request);

			Node node = meshRoot().getNodeRoot().findByUuid(restNode.getUuid());
			assertNotNull(node);
			assertThat(node).matches(request);

			// Load the node again
			restNode2 = call(() -> client().findNodeByUuid(PROJECT_NAME, restNode.getUuid(), new NodeParameters().setLanguages("de"),
					new VersioningParameters().draft()));

			// Delete the node
			MeshResponse<Void> deleteFut = client().deleteNode(PROJECT_NAME, restNode2.getUuid()).invoke();
			latchFor(deleteFut);
			assertSuccess(deleteFut);

			meshRoot().getNodeRoot().reload();
			Node deletedNode = meshRoot().getNodeRoot().findByUuid(restNode2.getUuid());
			assertNull("The node should have been deleted.", deletedNode);
		}
	}

	@Test
	public void testCreateNodeWithMissingParentNodeUuid() throws Exception {
		try (NoTx noTx = db.noTx()) {
			NodeCreateRequest request = new NodeCreateRequest();
			SchemaReference schemaReference = new SchemaReference();
			schemaReference.setName("node");
			request.setSchema(schemaReference);
			request.getFields().put("name", FieldUtil.createStringField("some name"));
			request.getFields().put("filename", FieldUtil.createStringField("new-page.html"));
			request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
			request.setSchema(new SchemaReference().setName("content").setUuid(schemaContainer("content").getUuid()));

			MeshResponse<NodeResponse> future = client().createNode(PROJECT_NAME, request).invoke();
			latchFor(future);
			expectException(future, BAD_REQUEST, "node_missing_parentnode_field");
		}
	}

	@Test
	public void testCreateNodeWithMissingSchemaPermission() {
		try (NoTx noTx = db.noTx()) {
			Node node = folder("news");
			String uuid = node.getUuid();
			role().revokePermissions(schemaContainer("content"), READ_PERM);

			NodeCreateRequest request = new NodeCreateRequest();
			SchemaReference schemaReference = new SchemaReference().setName("content").setUuid(schemaContainer("content").getUuid());
			request.setSchema(schemaReference);
			request.getFields().put("name", FieldUtil.createStringField("some name"));
			request.getFields().put("filename", FieldUtil.createStringField("new-page.html"));
			request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
			request.setSchema(new SchemaReference().setName("content").setUuid(schemaContainer("content").getUuid()));
			request.setLanguage("en");
			request.setParentNodeUuid(uuid);

			MeshResponse<NodeResponse> future = client().createNode(PROJECT_NAME, request).invoke();
			latchFor(future);
			expectException(future, FORBIDDEN, "error_missing_perm", schemaContainer("content").getUuid());
		}
	}

	@Test
	public void testCreateNodeWithMissingPermission() throws Exception {

		try (NoTx noTx = db.noTx()) {
			// Revoke create perm
			role().revokePermissions(folder("news"), CREATE_PERM);
		}

		try (NoTx noTx = db.noTx()) {
			Node node = folder("news");
			String uuid = node.getUuid();
			NodeCreateRequest request = new NodeCreateRequest();
			SchemaReference schemaReference = new SchemaReference().setName("content").setUuid(schemaContainer("content").getUuid());
			request.setSchema(schemaReference);
			request.getFields().put("name", FieldUtil.createStringField("some name"));
			request.getFields().put("filename", FieldUtil.createStringField("new-page.html"));
			request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
			request.setSchema(new SchemaReference().setName("content").setUuid(schemaContainer("content").getUuid()));
			request.setLanguage("en");
			request.setParentNodeUuid(uuid);

			assertThat(dummySearchProvider).recordedStoreEvents(0);
			MeshResponse<NodeResponse> future = client().createNode(PROJECT_NAME, request).invoke();
			latchFor(future);
			expectException(future, FORBIDDEN, "error_missing_perm", uuid);
			assertThat(dummySearchProvider).recordedStoreEvents(0);
		}
	}

	// Read tests

	/**
	 * Test default paging parameters
	 * 
	 * @throws Exception
	 */
	@Test
	public void testReadNodesDefaultPaging() throws Exception {
		NodeListResponse restResponse = call(() -> client().findNodes(PROJECT_NAME, new VersioningParameters().draft()));

		assertNotNull(restResponse);
		assertEquals(25, restResponse.getMetainfo().getPerPage());
		assertEquals(1, restResponse.getMetainfo().getCurrentPage());
		assertEquals(getNodeCount(), restResponse.getData().size());
	}

	@Test
	public void testReadMultipleAndAssertOrder() {
		try (NoTx noTx = db.noTx()) {
			Node parentNode = folder("2015");
			int nNodes = 20;
			for (int i = 0; i < nNodes; i++) {
				Node node = parentNode.create(user(), schemaContainer("content").getLatestVersion(), project());
				assertNotNull(node);
				role().grantPermissions(node, READ_PERM);
			}

			String firstUuid = null;
			for (int i = 0; i < 10; i++) {
				NodeListResponse response = call(
						() -> client().findNodes(PROJECT_NAME, new PagingParametersImpl(1, 100), new VersioningParameters().draft()));
				if (firstUuid == null) {
					firstUuid = response.getData().get(0).getUuid();
				}
				assertEquals("The first element in the page should not change but it changed in run {" + i + "}", firstUuid,
						response.getData().get(0).getUuid());
			}
		}

	}

	@Test
	@Override
	public void testReadMultiple() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Node parentNode = folder("2015");
			// Don't grant permissions to the no perm node. We want to make sure that this one will not be listed.
			Node noPermNode = parentNode.create(user(), schemaContainer("content").getLatestVersion(), project());
			String noPermNodeUUID = noPermNode.getUuid();

			// Create 20 drafts
			int nNodes = 20;
			for (int i = 0; i < nNodes; i++) {
				NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
				nodeCreateRequest.setSchema(new SchemaReference().setName("content"));
				nodeCreateRequest.getFields().put("name", FieldUtil.createStringField("test"));
				nodeCreateRequest.setParentNodeUuid(parentNode.getUuid());
				nodeCreateRequest.setLanguage("en");
				call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest));
			}

			assertNotNull(noPermNode.getUuid());
			int perPage = 11;
			NodeListResponse restResponse = call(
					() -> client().findNodes(PROJECT_NAME, new PagingParametersImpl(3, perPage), new VersioningParameters().draft()));
			assertEquals(perPage, restResponse.getData().size());

			// Extra Nodes + permitted nodes
			int totalNodes = getNodeCount() + nNodes;
			int totalPages = (int) Math.ceil(totalNodes / (double) perPage);
			assertEquals("The response did not contain the correct amount of items", perPage, restResponse.getData().size());
			assertEquals(3, restResponse.getMetainfo().getCurrentPage());
			assertEquals(totalNodes, restResponse.getMetainfo().getTotalCount());
			assertEquals(totalPages, restResponse.getMetainfo().getPageCount());
			assertEquals(perPage, restResponse.getMetainfo().getPerPage());

			List<NodeResponse> allNodes = new ArrayList<>();
			for (int page = 1; page <= totalPages; page++) {
				MeshResponse<NodeListResponse> pageFuture = client()
						.findNodes(PROJECT_NAME, new PagingParametersImpl(page, perPage), new VersioningParameters().draft()).invoke();
				latchFor(pageFuture);
				assertSuccess(pageFuture);
				restResponse = pageFuture.result();
				allNodes.addAll(restResponse.getData());
			}
			assertEquals("Somehow not all nodes were loaded when loading all pages.", totalNodes, allNodes.size());

			// Verify that the no_perm_node is not part of the response
			List<NodeResponse> filteredUserList = allNodes.parallelStream().filter(restNode -> restNode.getUuid().equals(noPermNodeUUID))
					.collect(Collectors.toList());
			assertTrue("The no perm node should not be part of the list since no permissions were added.", filteredUserList.size() == 0);

			MeshResponse<NodeListResponse> pageFuture = client().findNodes(PROJECT_NAME, new PagingParametersImpl(-1, 25)).invoke();
			latchFor(pageFuture);
			expectException(pageFuture, BAD_REQUEST, "error_page_parameter_must_be_positive", "-1");

			pageFuture = client().findNodes(PROJECT_NAME, new PagingParametersImpl(0, 25)).invoke();
			latchFor(pageFuture);
			expectException(pageFuture, BAD_REQUEST, "error_page_parameter_must_be_positive", "0");

			pageFuture = client().findNodes(PROJECT_NAME, new PagingParametersImpl(1, -1)).invoke();
			latchFor(pageFuture);
			expectException(pageFuture, BAD_REQUEST, "error_pagesize_parameter", "-1");

			NodeListResponse list = call(
					() -> client().findNodes(PROJECT_NAME, new PagingParametersImpl(4242, 25), new VersioningParameters().draft()));
			assertEquals(4242, list.getMetainfo().getCurrentPage());
			assertEquals(0, list.getData().size());
			assertEquals(25, list.getMetainfo().getPerPage());
			assertEquals(2, list.getMetainfo().getPageCount());
			assertEquals(getNodeCount() + nNodes, list.getMetainfo().getTotalCount());
		}
	}

	@Test
	public void testReadMultipleOnlyMetadata() {
		NodeListResponse listResponse = call(
				() -> client().findNodes(PROJECT_NAME, new PagingParametersImpl(1, 0), new VersioningParameters().draft()));
		assertEquals(0, listResponse.getData().size());
	}

	@Test
	public void testReadNodesWithoutPermissions() throws Exception {

		// TODO add node that has no perms and check the response
		NodeListResponse restResponse = call(
				() -> client().findNodes(PROJECT_NAME, new PagingParametersImpl(1, 10), new VersioningParameters().draft()));

		int nElements = restResponse.getData().size();
		assertEquals("The amount of elements in the list did not match the expected count", 10, nElements);
		assertEquals(1, restResponse.getMetainfo().getCurrentPage());
		assertEquals(2, restResponse.getMetainfo().getPageCount());
		assertEquals(10, restResponse.getMetainfo().getPerPage());
		assertEquals(getNodeCount(), restResponse.getMetainfo().getTotalCount());
	}

	@Test
	public void testReadNodesForRelease() {
		try (NoTx noTx = db.noTx()) {
			Project project = project();
			Release initialRelease = project.getInitialRelease();
			Release newRelease = project.getReleaseRoot().create("newrelease", user());

			NodeListResponse restResponse = call(
					() -> client().findNodes(PROJECT_NAME, new PagingParametersImpl(1, 1000), new VersioningParameters().draft()));
			assertThat(restResponse.getData()).as("Node List for latest release").isEmpty();

			restResponse = call(() -> client().findNodes(PROJECT_NAME, new PagingParametersImpl(1, 1000),
					new VersioningParameters().setRelease(initialRelease.getName()).draft()));
			assertThat(restResponse.getData()).as("Node List for initial release").hasSize(getNodeCount());

			// update a single node in the new release
			Node node = folder("2015");
			NodeUpdateRequest update = new NodeUpdateRequest();
			update.setLanguage("en");
			update.getFields().put("name", FieldUtil.createStringField("2015 new release"));
			call(() -> client().updateNode(PROJECT_NAME, node.getUuid(), update));

			// check whether there is one node in the new release now
			restResponse = call(() -> client().findNodes(PROJECT_NAME, new PagingParametersImpl(1, 1000), new VersioningParameters().draft()));
			assertThat(restResponse.getData()).as("Node List for latest release").hasSize(1);

			restResponse = call(() -> client().findNodes(PROJECT_NAME, new PagingParametersImpl(1, 1000),
					new VersioningParameters().draft().setRelease(newRelease.getName())));
			assertThat(restResponse.getData()).as("Node List for latest release").hasSize(1);
		}
	}

	@Test
	public void testReadPublishedNodes() {
		try (NoTx noTx = db.noTx()) {
			// 1. Take all nodes offline
			call(() -> client().takeNodeOffline(PROJECT_NAME, project().getBaseNode().getUuid(), new PublishParameters().setRecursive(true)));

			// 2. Assert that all nodes are offline. The findNodes method should not find any node because it searches for published nodes by default.
			NodeListResponse listResponse = call(() -> client().findNodes(PROJECT_NAME, new PagingParametersImpl(1, 1000)));
			assertThat(listResponse.getData()).as("Published nodes list").isEmpty();

			List<Node> nodes = Arrays.asList(folder("products"), folder("deals"), folder("news"), folder("2015"));
			nodes.stream().forEach(node -> call(() -> client().publishNode(PROJECT_NAME, node.getUuid())));

			List<NodeResponse> publishedNodes = nodes.stream().map(node -> call(() -> client().findNodeByUuid(PROJECT_NAME, node.getUuid())))
					.collect(Collectors.toList());
			assertThat(publishedNodes).hasSize(nodes.size());

			listResponse = call(() -> client().findNodes(PROJECT_NAME, new PagingParametersImpl(1, 1000)));
			assertThat(listResponse.getData()).as("Published nodes list").usingElementComparatorOnFields("uuid")
					.containsOnlyElementsOf(publishedNodes);
		}
	}

	@Test
	public void testReadPublishedNodesNoPermission() {
		try (NoTx noTx = db.noTx()) {

			// Take all nodes offline
			call(() -> client().takeNodeOffline(PROJECT_NAME, project().getBaseNode().getUuid(), new PublishParameters().setRecursive(true)));

			NodeListResponse listResponse = call(() -> client().findNodes(PROJECT_NAME, new PagingParametersImpl(1, 1000)));
			assertThat(listResponse.getData()).as("Published nodes list").isEmpty();

			List<Node> nodes = new ArrayList<>(Arrays.asList(folder("products"), folder("deals"), folder("news"), folder("2015")));
			nodes.stream().forEach(node -> call(() -> client().publishNode(PROJECT_NAME, node.getUuid())));

			// revoke permission on one folder after the other
			while (!nodes.isEmpty()) {
				Node folder = nodes.remove(0);
				db.tx(() -> {
					role().revokePermissions(folder, READ_PUBLISHED_PERM);
					return null;
				});

				List<NodeResponse> publishedNodes = nodes.stream().map(node -> call(() -> client().findNodeByUuid(PROJECT_NAME, node.getUuid())))
						.collect(Collectors.toList());
				assertThat(publishedNodes).hasSize(nodes.size());

				listResponse = call(() -> client().findNodes(PROJECT_NAME, new PagingParametersImpl(1, 1000)));
				assertThat(listResponse.getData()).as("Published nodes list").usingElementComparatorOnFields("uuid")
						.containsOnlyElementsOf(publishedNodes);
			}
		}
	}

	@Test
	@Ignore("Disabled until custom 404 handler has been added")
	public void testReadNodeWithBogusProject() {
		MeshResponse<NodeResponse> future = client().findNodeByUuid("BOGUS", "someUuuid").invoke();
		latchFor(future);
		expectException(future, BAD_REQUEST, "project_not_found", "BOGUS");
	}

	@Test
	@Ignore("Disabled since test is unstable - CL-246")
	public void testCreateUpdateReadDeleteMultithreaded() throws Exception {

		Logger log = LoggerFactory.getLogger(NodeEndpointTest.class);

		int nJobs = 200;
		try (NoTx noTx = db.noTx()) {
			CountDownLatch latch = new CountDownLatch(nJobs);

			Node parentNode = folder("news");
			String uuid = parentNode.getUuid();

			long nNodesFound = meshRoot().getNodeRoot().findAll().size();

			NodeCreateRequest createRequest = new NodeCreateRequest();
			createRequest.setSchema(new SchemaReference().setName("content").setUuid(schemaContainer("content").getUuid()));
			createRequest.setLanguage("en");
			createRequest.getFields().put("title", FieldUtil.createStringField("some title"));
			createRequest.getFields().put("name", FieldUtil.createStringField("some name"));
			createRequest.getFields().put("filename", FieldUtil.createStringField("new-page.html"));
			createRequest.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
			createRequest.setParentNodeUuid(uuid);

			NodeUpdateRequest updateRequest = new NodeUpdateRequest();
			SchemaReference schemaReference = new SchemaReference();
			schemaReference.setName("folder");
			schemaReference.setUuid(schemaContainer("folder").getUuid());
			updateRequest.setSchema(schemaReference);
			updateRequest.setLanguage("en");
			updateRequest.getFields().put("name", FieldUtil.createStringField("UPDATED"));

			// Create various nodes and update them directly after creation. Ensure that update was successful.
			for (int i = 0; i < nJobs; i++) {
				log.info("Invoking createNode REST call for job {" + i + "}");
				MeshResponse<NodeResponse> createFuture = client().createNode(PROJECT_NAME, createRequest).invoke();

				createFuture.setHandler(rh -> {
					if (rh.failed()) {
						fail(rh.cause().getMessage());
					} else {
						log.info("Created {" + rh.result().getUuid() + "}");
						NodeResponse response = rh.result();
						MeshResponse<NodeResponse> updateFuture = client().updateNode(PROJECT_NAME, response.getUuid(), updateRequest).invoke();
						updateFuture.setHandler(uh -> {
							if (uh.failed()) {
								fail(uh.cause().getMessage());
							} else {
								log.info("Updated {" + uh.result().getUuid() + "}");
								MeshResponse<NodeResponse> readFuture = client()
										.findNodeByUuid(PROJECT_NAME, uh.result().getUuid(), new VersioningParameters().draft()).invoke();
								readFuture.setHandler(rf -> {
									if (rh.failed()) {
										fail(rh.cause().getMessage());
									} else {
										log.info("Read {" + rf.result().getUuid() + "}");
										MeshResponse<Void> deleteFuture = client().deleteNode(PROJECT_NAME, rf.result().getUuid()).invoke();
										deleteFuture.setHandler(df -> {
											if (df.failed()) {
												fail(df.cause().getMessage());
											} else {
												log.info("Deleted {" + rf.result().getUuid() + "}");
												latch.countDown();
											}
										});
									}

								});

							}
						});
					}
				});
				Thread.sleep(250);
				log.info("Invoked call create requests.");
			}

			failingLatch(latch);

			long nNodesFoundAfterRest = meshRoot().getNodeRoot().findAll().size();
			assertEquals("All created nodes should have been created.", nNodesFound, nNodesFoundAfterRest);
			// for (Future<NodeResponse> future : set) {
			// latchFor(future);
			// assertSuccess(future);
			// }
		}
	}

	@Test
	@Override
	public void testCreateMultithreaded() throws InterruptedException {
		try (NoTx noTx = db.noTx()) {
			Node parentNode = folder("news");
			String uuid = parentNode.getUuid();
			assertNotNull(parentNode);
			assertNotNull(parentNode.getUuid());

			int nJobs = 500;
			// CyclicBarrier barrier = new CyclicBarrier(nJobs);
			// Trx.enableDebug();
			// Trx.setBarrier(barrier);
			Set<MeshResponse<NodeResponse>> set = new HashSet<>();
			final AtomicInteger e = new AtomicInteger(0);
			for (int i = 0; i < nJobs; i++) {
				new Thread(() -> {
					NodeCreateRequest request = new NodeCreateRequest();
					request.setSchema(new SchemaReference().setName("content"));
					request.setLanguage("en");
					request.getFields().put("title", FieldUtil.createStringField("some title"));
					request.getFields().put("name", FieldUtil.createStringField("some name"));
					request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
					request.setParentNodeUuid(uuid);
					request.getFields().put("filename", FieldUtil.createStringField("new-page" + e.incrementAndGet() + ".html"));
					set.add(client().createNode(PROJECT_NAME, request).invoke());
				}).start();
			}

			Thread.sleep(10000);
			//
			//			// Check each call response
			Set<String> uuids = new HashSet<>();
			for (MeshResponse<NodeResponse> future : set) {
				latchFor(future);
				assertSuccess(future);
				String currentUuid = future.result().getUuid();
				assertFalse("The rest api returned a node response with a uuid that was returned before. Each create request must always be atomic.",
						uuids.contains(currentUuid));
				uuids.add(currentUuid);
			}
			// Trx.disableDebug();
			// assertFalse("The barrier should not break. Somehow not all threads reached the barrier point.", barrier.isBroken());
		}
	}

	@Test
	@Override
	@Ignore("Multithreaded update is currently only possible for multiple nodes not a single node")
	public void testUpdateMultithreaded() throws InterruptedException {

		final String newName = "english renamed name";
		String uuid = db.noTx(() -> folder("2015").getUuid());
		assertEquals("2015", db.noTx(() -> folder("2015").getLatestDraftFieldContainer(english()).getString("name").getString()));
		VersionNumber version = db.noTx(() -> folder("2015").getLatestDraftFieldContainer(english()).getVersion());

		NodeUpdateRequest request = new NodeUpdateRequest();
		SchemaReference schemaReference = new SchemaReference();
		schemaReference.setName("folder");
		request.setSchema(schemaReference);
		request.setLanguage("en");

		NodeParameters parameters = new NodeParameters();
		parameters.setLanguages("en", "de");

		int nJobs = 5;
		// CyclicBarrier barrier = new CyclicBarrier(nJobs);
		// Trx.enableDebug();
		// Trx.setBarrier(barrier);
		Set<MeshResponse<NodeResponse>> set = new HashSet<>();
		for (int i = 0; i < nJobs; i++) {
			System.out.println(version.getFullVersion());
			request.setVersion(new VersionReference().setNumber(version.getFullVersion()));
			request.getFields().put("name", FieldUtil.createStringField(newName + ":" + i));
			set.add(client().updateNode(PROJECT_NAME, uuid, request, parameters).invoke());
			//			version = version.nextDraft();
			//			VersionNumber currentVersion = db.noTx(() -> folder("2015").getLatestDraftFieldContainer(english()).getVersion());
			//			System.out.println("CurrentVersion: " + currentVersion.getFullVersion());
		}

		for (MeshResponse<NodeResponse> response : set) {
			latchFor(response);
			assertSuccess(response);
		}
		// Trx.disableDebug();
		// assertFalse("The barrier should not break. Somehow not all threads reached the barrier point.", barrier.isBroken());

	}

	@Test
	@Override
	public void testDeleteByUUIDMultithreaded() {

		String uuid = db.noTx(() -> folder("2015").getUuid());
		int nJobs = 6;

		// CyclicBarrier barrier = new CyclicBarrier(nJobs);
		// Trx.enableDebug();
		// Trx.setBarrier(barrier);
		Set<MeshResponse<Void>> set = new HashSet<>();
		for (int i = 0; i < nJobs; i++) {
			set.add(client().deleteNode(PROJECT_NAME, uuid).invoke());
		}

		validateDeletion(set, null);
		// call(() -> getClient().deleteNode(PROJECT_NAME, uuid));

	}

	@Test
	@Override
	public void testReadByUuidMultithreaded() throws InterruptedException {
		int nJobs = 50;
		try (NoTx noTx = db.noTx()) {
			Set<MeshResponse<NodeResponse>> set = new HashSet<>();
			for (int i = 0; i < nJobs; i++) {
				set.add(client().findNodeByUuid(PROJECT_NAME, folder("2015").getUuid(), new VersioningParameters().draft()).invoke());
			}
			for (MeshResponse<NodeResponse> future : set) {
				latchFor(future);
				assertSuccess(future);
			}
		}
	}

	@Test
	@Override
	public void testReadByUuidMultithreadedNonBlocking() throws InterruptedException {
		int nJobs = 200;
		try (NoTx noTx = db.noTx()) {
			Set<MeshResponse<NodeResponse>> set = new HashSet<>();
			for (int i = 0; i < nJobs; i++) {
				set.add(client().findNodeByUuid(PROJECT_NAME, folder("2015").getUuid(), new VersioningParameters().draft()).invoke());
			}
			for (MeshResponse<NodeResponse> future : set) {
				latchFor(future);
				assertSuccess(future);
			}
			// Trx.disableDebug();
		}
	}

	@Test
	public void testReadByUuidWithRolePerms() {
		try (NoTx noTx = db.noTx()) {
			Node node = folder("2015");
			String uuid = node.getUuid();

			NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid,
					new RolePermissionParameters().setRoleUuid(role().getUuid()), new VersioningParameters().draft()));
			assertNotNull(response.getRolePerms());
			assertEquals(6, response.getRolePerms().length);
		}
	}

	@Test
	public void testReadByUUID() throws Exception {
		try (NoTx noTx = db.noTx()) {
			String releaseUuid = project().getLatestRelease().getUuid();
			Node node = folder("2015");
			String uuid = node.getUuid();
			assertNotNull(node);
			assertNotNull(node.getUuid());

			NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParameters().draft()));
			assertThat(folder("2015")).matches(response);

			assertNotNull(response.getParentNode());
			assertEquals(folder("2015").getParentNode(releaseUuid).getUuid(), response.getParentNode().getUuid());
			assertEquals("News", response.getParentNode().getDisplayName());
			assertEquals("en", response.getLanguage());
		}
	}

	@Test
	public void testReadVersionByNumber() {
		try (NoTx noTx = db.noTx()) {
			Node node = folder("2015");
			String uuid = node.getUuid();

			// Load node and assert initial versions and field values
			NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParameters().draft()));
			assertThat(response).hasVersion("1.0").hasLanguage("en").hasStringField("name", "2015");

			// create version 1.1
			NodeUpdateRequest updateRequest = new NodeUpdateRequest();
			updateRequest.setLanguage("en");
			updateRequest.setVersion(new VersionReference(null, "1.0"));
			updateRequest.getFields().put("name", FieldUtil.createStringField("one"));
			call(() -> client().updateNode(PROJECT_NAME, uuid, updateRequest));

			// create version 1.2
			updateRequest.setVersion(new VersionReference(null, "1.1"));
			updateRequest.getFields().put("name", FieldUtil.createStringField("two"));
			call(() -> client().updateNode(PROJECT_NAME, uuid, updateRequest));

			// create version 1.3
			updateRequest.setVersion(new VersionReference(null, "1.2"));
			updateRequest.getFields().put("name", FieldUtil.createStringField("three"));
			call(() -> client().updateNode(PROJECT_NAME, uuid, updateRequest));

			// create german version 0.1
			updateRequest.setLanguage("de");
			updateRequest.setVersion(null);
			updateRequest.getFields().put("name", FieldUtil.createStringField("eins"));
			call(() -> client().updateNode(PROJECT_NAME, uuid, updateRequest));

			// create german version 0.2
			updateRequest.setVersion(new VersionReference(null, "0.1"));
			updateRequest.getFields().put("name", FieldUtil.createStringField("zwei"));
			call(() -> client().updateNode(PROJECT_NAME, uuid, updateRequest));

			// create german version 0.3
			updateRequest.setVersion(new VersionReference(null, "0.2"));
			updateRequest.getFields().put("name", FieldUtil.createStringField("drei"));
			call(() -> client().updateNode(PROJECT_NAME, uuid, updateRequest));

			// Test english versions
			assertThat(call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParameters().draft()))).as("Draft").hasVersion("1.3")
					.hasLanguage("en").hasStringField("name", "three");

			assertThat(call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParameters().setVersion("1.0")))).as("Version 1.0")
					.hasVersion("1.0").hasLanguage("en").hasStringField("name", "2015");

			assertThat(call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParameters().setVersion("1.1")))).as("Version 1.1")
					.hasVersion("1.1").hasLanguage("en").hasStringField("name", "one");
			assertThat(call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParameters().setVersion("1.2")))).as("Version 1.2")
					.hasVersion("1.2").hasLanguage("en").hasStringField("name", "two");
			assertThat(call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParameters().setVersion("1.3")))).as("Version 1.3")
					.hasVersion("1.3").hasLanguage("en").hasStringField("name", "three");

			// Test german versions
			assertThat(call(
					() -> client().findNodeByUuid(PROJECT_NAME, uuid, new NodeParameters().setLanguages("de"), new VersioningParameters().draft())))
							.as("German draft").hasVersion("0.3").hasLanguage("de").hasStringField("name", "drei");
			assertThat(call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new NodeParameters().setLanguages("de"),
					new VersioningParameters().setVersion("0.1")))).as("German version 0.1").hasVersion("0.1").hasLanguage("de")
							.hasStringField("name", "eins");
			assertThat(call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new NodeParameters().setLanguages("de"),
					new VersioningParameters().setVersion("0.2")))).as("German version 0.2").hasVersion("0.2").hasLanguage("de")
							.hasStringField("name", "zwei");
			assertThat(call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new NodeParameters().setLanguages("de"),
					new VersioningParameters().setVersion("0.3")))).as("German version 0.3").hasVersion("0.3").hasLanguage("de")
							.hasStringField("name", "drei");
		}
	}

	@Test
	public void testReadBogusVersion() {
		try (NoTx noTx = db.noTx()) {
			Node node = folder("2015");
			String uuid = node.getUuid();

			call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParameters().setVersion("bogus")), BAD_REQUEST,
					"error_illegal_version", "bogus");
		}
	}

	@Test
	public void testReadInexistentVersion() {
		try (NoTx noTx = db.noTx()) {
			Node node = folder("2015");
			String uuid = node.getUuid();

			call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParameters().setVersion("47.11")), NOT_FOUND,
					"object_not_found_for_version", "47.11");
		}
	}

	@Test
	public void testReadPublishedVersion() {
		try (NoTx noTx = db.noTx()) {
			Node node = folder("2015");
			String uuid = node.getUuid();

			// 1. Take node offline
			call(() -> client().takeNodeOffline(PROJECT_NAME, uuid, new PublishParameters().setRecursive(true)));

			// 2. Load load using default options. By default the scope published is active. Thus the node can't be found.
			call(() -> client().findNodeByUuid(PROJECT_NAME, uuid), NOT_FOUND, "node_error_published_not_found_for_uuid_release_version", uuid,
					project().getLatestRelease().getUuid());

			// 3. Publish the node again.
			call(() -> client().publishNode(PROJECT_NAME, uuid));

			// 4. Assert that the node can be found.
			NodeResponse nodeResponse = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid));
			assertThat(nodeResponse).as("Published node").hasLanguage("en").hasVersion("2.0");
		}
	}

	@Test
	public void testReadNodeForRelease() {
		try (NoTx noTx = db.noTx()) {
			Node node = folder("2015");
			String uuid = node.getUuid();
			Project project = project();
			Release initialRelease = project.getReleaseRoot().getInitialRelease();
			Release newRelease = project.getReleaseRoot().create("newrelease", user());

			NodeUpdateRequest updateRequest = new NodeUpdateRequest();
			updateRequest.setLanguage("en");
			updateRequest.getFields().put("name", FieldUtil.createStringField("2015 in new release"));
			call(() -> client().updateNode(PROJECT_NAME, uuid, updateRequest, new VersioningParameters().setRelease(newRelease.getName())));

			assertThat(
					call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParameters().setRelease(initialRelease.getName()).draft())))
							.as("Initial Release Version").hasVersion("1.0").hasStringField("name", "2015");
			assertThat(call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParameters().setRelease(newRelease.getName()).draft())))
					.as("New Release Version").hasVersion("0.1").hasStringField("name", "2015 in new release");
		}
	}

	@Test
	public void testReadNodeVersionForRelease() {
		try (NoTx noTx = db.noTx()) {
			Node node = folder("2015");
			String uuid = node.getUuid();
			Project project = project();
			Release initialRelease = project.getReleaseRoot().getInitialRelease();
			Release newRelease = project.getReleaseRoot().create("newrelease", user());

			NodeUpdateRequest updateRequest = new NodeUpdateRequest();
			updateRequest.setLanguage("en");

			// create version 0.1 in new release
			updateRequest.getFields().put("name", FieldUtil.createStringField("2015 v0.1 new release"));
			NodeResponse response = call(
					() -> client().updateNode(PROJECT_NAME, uuid, updateRequest, new VersioningParameters().setRelease(newRelease.getName())));
			assertEquals("0.1", response.getVersion().getNumber());

			// create version 1.1 in initial release (1.0 is the current published en node)
			updateRequest.getFields().put("name", FieldUtil.createStringField("2015 v1.1 initial release"));
			updateRequest.setVersion(new VersionReference(null, "1.0"));
			response = call(
					() -> client().updateNode(PROJECT_NAME, uuid, updateRequest, new VersioningParameters().setRelease(initialRelease.getName())));
			assertEquals("1.1", response.getVersion().getNumber());

			// create version 0.2 in new release
			updateRequest.getFields().put("name", FieldUtil.createStringField("2015 v0.2 new release"));
			updateRequest.setVersion(new VersionReference(null, "0.1"));
			response = call(
					() -> client().updateNode(PROJECT_NAME, uuid, updateRequest, new VersioningParameters().setRelease(newRelease.getName())));
			assertEquals("0.2", response.getVersion().getNumber());

			assertThat(call(() -> client().findNodeByUuid(PROJECT_NAME, uuid,
					new VersioningParameters().setRelease(initialRelease.getName()).setVersion("0.1")))).as("Initial Release Version")
							.hasVersion("0.1").hasStringField("name", "2015");
			assertThat(call(
					() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParameters().setRelease(newRelease.getName()).setVersion("0.1"))))
							.as("New Release Version").hasVersion("0.1").hasStringField("name", "2015 v0.1 new release");
			assertThat(call(() -> client().findNodeByUuid(PROJECT_NAME, uuid,
					new VersioningParameters().setRelease(initialRelease.getName()).setVersion("1.1")))).as("Initial Release Version")
							.hasVersion("1.1").hasStringField("name", "2015 v1.1 initial release");
			assertThat(call(
					() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParameters().setRelease(newRelease.getName()).setVersion("0.2"))))
							.as("New Release Version").hasVersion("0.2").hasStringField("name", "2015 v0.2 new release");
		}
	}

	/**
	 * Test reading a node with link resolving enabled. Ensure that the schema segment field of the node is not set.
	 */
	@Test
	public void testReadByUUIDWithLinkPathsAndNoSegmentFieldRef() {
		try (NoTx noTx = db.noTx()) {
			Node node = folder("news");
			// Update the schema
			Schema schema = node.getSchemaContainer().getLatestVersion().getSchema();
			schema.setSegmentField(null);
			node.getSchemaContainer().getLatestVersion().setSchema(schema);
			MeshInternal.get().serverSchemaStorage().clear();

			NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, node.getUuid(),
					new NodeParameters().setResolveLinks(LinkType.FULL), new VersioningParameters().draft()));
			assertEquals("/api/v1/dummy/webroot/error/404", response.getPath());
			assertThat(response.getLanguagePaths()).containsEntry("en", "/api/v1/dummy/webroot/error/404");
			assertThat(response.getLanguagePaths()).containsEntry("de", "/api/v1/dummy/webroot/error/404");
		}
	}

	@Test
	public void testReadByUUIDWithLinkPaths() {
		try (NoTx noTx = db.noTx()) {
			Node node = folder("news");
			NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, node.getUuid(), new VersioningParameters().draft(),
					new NodeParameters().setResolveLinks(LinkType.FULL)));
			assertThat(response.getAvailableLanguages()).containsExactly("de", "en");
			assertThat(response.getLanguagePaths()).containsEntry("en", "/api/v1/dummy/webroot/News");
			assertThat(response.getLanguagePaths()).containsEntry("de", "/api/v1/dummy/webroot/Neuigkeiten");
		}
	}

	@Test
	public void testReadBreadcrumbWithLangfallback() {
		String baseNodeUuid = db.noTx(() -> project().getBaseNode().getUuid());

		// level 0
		NodeCreateRequest request = new NodeCreateRequest();
		request.setSchema(new SchemaReference().setName("folder"));
		request.setLanguage("en");
		request.getFields().put("name", FieldUtil.createStringField("english folder-0"));
		request.setParentNodeUuid(baseNodeUuid);
		NodeResponse response = call(() -> client().createNode(PROJECT_NAME, request));

		// level 1
		request.setParentNodeUuid(response.getUuid());
		request.getFields().put("name", FieldUtil.createStringField("english folder-1"));
		response = call(() -> client().createNode(PROJECT_NAME, request));

		// level 2
		request.setLanguage("de");
		request.setParentNodeUuid(response.getUuid());
		request.getFields().put("name", FieldUtil.createStringField("german folder-2"));
		response = call(() -> client().createNode(PROJECT_NAME, request));

		// Load the german folder
		String uuid = response.getUuid();
		response = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid,
				new NodeParameters().setResolveLinks(LinkType.FULL).setLanguages("de", "en"), new VersioningParameters().setVersion("draft")));

		assertEquals("/api/v1/dummy/webroot/english%20folder-0/english%20folder-1/german%20folder-2", response.getPath());
		assertEquals("/api/v1/dummy/webroot/english%20folder-0/english%20folder-1", response.getBreadcrumb().getFirst().getPath());
		assertEquals("/api/v1/dummy/webroot/english%20folder-0", response.getBreadcrumb().getLast().getPath());

	}

	@Test
	public void testReadByUUIDBreadcrumb() {
		try (NoTx noTx = db.noTx()) {
			Node node = content("news_2014");
			NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, node.getUuid(),
					new NodeParameters().setResolveLinks(LinkType.FULL), new VersioningParameters().draft()));
			assertTrue(response.getBreadcrumb().getFirst().getUuid().equals(folder("2014").getUuid()));
			assertTrue(response.getBreadcrumb().getFirst().getDisplayName().equals("2014"));
			assertTrue(response.getBreadcrumb().getLast().getUuid().equals(folder("news").getUuid()));
			assertTrue(response.getBreadcrumb().getLast().getDisplayName().equals("News"));
			assertEquals("/api/v1/dummy/webroot/News/2014", response.getBreadcrumb().getFirst().getPath());
			assertEquals("/api/v1/dummy/webroot/News", response.getBreadcrumb().getLast().getPath());
			assertEquals("Only two items should be listed in the breadcrumb", 2, response.getBreadcrumb().size());

			response = call(() -> client().findNodeByUuid(PROJECT_NAME, node.getUuid(), new VersioningParameters().draft()));
			assertTrue(response.getBreadcrumb().getFirst().getUuid().equals(folder("2014").getUuid()));
			assertTrue(response.getBreadcrumb().getFirst().getDisplayName().equals("2014"));
			assertTrue(response.getBreadcrumb().getLast().getUuid().equals(folder("news").getUuid()));
			assertTrue(response.getBreadcrumb().getLast().getDisplayName().equals("News"));
			assertNull("No path should be rendered since by default the linkType is OFF", response.getBreadcrumb().getFirst().getPath());
			assertNull("No path should be rendered since by default the linkType is OFF", response.getBreadcrumb().getLast().getPath());
			assertEquals("Only two items should be listed in the breadcrumb", 2, response.getBreadcrumb().size());
		}
	}

	@Test
	public void testReadBaseNode() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Node node = project().getBaseNode();
			String uuid = node.getUuid();

			NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid));
			assertNotNull(response);
			assertEquals("folder", response.getSchema().getName());
			assertThat(response.getAvailableLanguages()).containsExactly("en");
			assertEquals("en", response.getLanguage());

			response = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new NodeParameters().setResolveLinks(LinkType.FULL)));
			assertNotNull(response);
			assertEquals("folder", response.getSchema().getName());
			assertEquals("/api/v1/dummy/webroot/", response.getLanguagePaths().get("en"));
		}
	}

	@Test
	public void testReadNodeByUUIDLanguageFallback() {
		try (NoTx noTx = db.noTx()) {
			Node node = folder("products");
			SearchQueueBatch batch = createBatch();
			node.getLatestDraftFieldContainer(english()).delete(batch);
			String uuid = node.getUuid();

			// Request the node with various language parameter values. Fallback to "de"
			NodeParameters parameters = new NodeParameters();
			parameters.setLanguages("dv,nl,de,en");
			VersioningParameters versionParams = new VersioningParameters().draft();
			NodeResponse restNode = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, parameters, versionParams));
			assertThat(folder("products")).matches(restNode);

			// Ensure "de" version was returned
			StringField field = restNode.getFields().getStringField("name");
			String nameText = field.getString();
			assertEquals("Produkte", nameText);
		}

	}

	@Test
	public void testReadNodeByUUIDSingleLanguage() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Node node = folder("products");
			String uuid = node.getUuid();

			NodeParameters parameters = new NodeParameters();
			parameters.setLanguages("de");
			VersioningParameters versionParams = new VersioningParameters().draft();
			NodeResponse restNode = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, parameters, versionParams));
			assertThat(folder("products")).matches(restNode);

			StringField field = restNode.getFields().getStringField("name");
			String nameText = field.getString();
			assertEquals("Produkte", nameText);
		}
	}

	@Test
	public void testReadNodeByUUIDNoLanguage() throws Exception {
		try (NoTx noTx = db.noTx()) {
			// Create node with nl language
			Node parentNode = folder("products");
			Language languageNl = meshRoot().getLanguageRoot().findByLanguageTag("nl");
			SchemaContainerVersion version = schemaContainer("content").getLatestVersion();
			Node node = parentNode.create(user(), version, project());
			NodeGraphFieldContainer englishContainer = node.createGraphFieldContainer(languageNl, node.getProject().getLatestRelease(), user());
			englishContainer.createString("name").setString("name");
			englishContainer.createString("title").setString("title");
			englishContainer.createString("displayName").setString("displayName");
			englishContainer.createString("filename").setString("filename.nl.html");
			englishContainer.createHTML("content").setHtml("nl content");
			role().grantPermissions(node, READ_PERM);

			// Request the node in english en
			NodeParameters parameters = new NodeParameters();
			parameters.setLanguages("en");
			VersioningParameters versionParams = new VersioningParameters().draft();
			NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, node.getUuid(), parameters, versionParams));
			assertThat(response.getLanguage()).as("Node language").isNull();
			assertThat(response.getAvailableLanguages()).as("Available languages").containsOnly("nl");
			assertThat(response.getFields()).as("Node Fields").isEmpty();
		}
	}

	@Test
	public void testReadNodeWithBogusLanguageCode() throws Exception {
		try (NoTx noTx = db.noTx()) {

			Node node = folder("2015");
			String uuid = node.getUuid();
			assertNotNull(node);
			assertNotNull(node.getUuid());

			NodeParameters parameters = new NodeParameters();
			parameters.setLanguages("blabla", "edgsdg");
			VersioningParameters versionParams = new VersioningParameters().draft();

			assertThat(dummySearchProvider).recordedStoreEvents(0);
			MeshResponse<NodeResponse> future = client().findNodeByUuid(PROJECT_NAME, uuid, parameters, versionParams).invoke();
			latchFor(future);
			expectException(future, BAD_REQUEST, "error_language_not_found", "blabla");
			assertThat(dummySearchProvider).recordedStoreEvents(0);
		}
	}

	@Test
	@Override
	public void testReadByUUIDWithMissingPermission() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Node node = folder("2015");
			String uuid = node.getUuid();
			role().revokePermissions(node, READ_PERM);
			MeshResponse<NodeResponse> future = client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParameters().draft()).invoke();
			latchFor(future);
			expectException(future, FORBIDDEN, "error_missing_perm", uuid);
		}
	}

	@Test
	public void testReadNodeByBogusUUID() throws Exception {
		MeshResponse<NodeResponse> future = client().findNodeByUuid(PROJECT_NAME, "bogusUUID").invoke();
		latchFor(future);
		expectException(future, NOT_FOUND, "object_not_found_for_uuid", "bogusUUID");
	}

	@Test
	public void testReadNodeByInvalidUUID() throws Exception {
		String uuid = "dde8ba06bb7211e4897631a9ce2772f5";
		MeshResponse<NodeResponse> future = client().findNodeByUuid(PROJECT_NAME, uuid).invoke();
		latchFor(future);
		expectException(future, NOT_FOUND, "object_not_found_for_uuid", uuid);
	}

	// Update

	@Test
	@Override
	public void testUpdate() throws GenericRestException, Exception {
		final String newName = "english renamed name";

		// 1. Load Ids / Objects
		String uuid = db.noTx(() -> content("concorde").getUuid());
		Node node = db.noTx(() -> content("concorde"));
		NodeGraphFieldContainer origContainer = db.noTx(() -> {
			Node prod = content("concorde");
			NodeGraphFieldContainer container = prod.getLatestDraftFieldContainer(english());
			assertEquals("Concorde_english_name", container.getString("name").getString());
			assertEquals("Concorde english title", container.getString("title").getString());
			UserInfo userInfo = dataProvider.createUserInfo("dummy", "Dummy Firstname", "Dummy Lastname");
			group().addUser(userInfo.getUser());
			return container;
		});

		// Now login with a different user to see that the editor field gets updated correctly
		client().logout().toBlocking().value();
		client().setLogin("dummy", "test123");
		client().login().toBlocking().value();

		// 2. Prepare the update request (change name field)
		NodeUpdateRequest request = new NodeUpdateRequest();
		SchemaReference schemaReference = new SchemaReference().setName("content");
		request.setSchema(schemaReference);
		request.setLanguage("en");
		request.setVersion(new VersionReference(null, "0.1"));
		request.getFields().put("name", FieldUtil.createStringField(newName));

		// 3. Invoke update
		NodeResponse restNode = call(() -> client().updateNode(PROJECT_NAME, uuid, request, new NodeParameters().setLanguages("en", "de")));
		// Assert updater information
		assertEquals("Dummy Firstname", restNode.getEditor().getFirstName());
		assertEquals("Dummy Lastname", restNode.getEditor().getLastName());

		// 4. Assert that new version 1.1 was created. (1.0 was the published 0.1 draft)
		assertThat(restNode).as("update response").isNotNull().hasLanguage("en").hasVersion("1.1").hasStringField("name", newName)
				.hasStringField("title", "Concorde english title");

		// 5. Assert graph changes
		try (NoTx noTx = db.noTx()) {
			node = content("concorde");
			node.reload();
			origContainer.reload();

			// First check whether the objects we check are the correct ones
			assertEquals("The original container should be 1.0 (the latest published version)", "1.0", origContainer.getVersion().toString());
			NodeGraphFieldContainer container = node.getLatestDraftFieldContainer(english());
			container.reload();
			assertEquals("The loaded container did not match the latest version.", "1.1", container.getVersion().toString());

			// Assert applied changes
			assertEquals("The string field was not updated within the new container", newName, container.getString("name").getString());
			assertEquals("Concorde english title", container.getString("title").getString());

			// Assert that the containers were linked together as expected
			// 0.1 -> 1.0 -> 1.1
			assertThat(container).as("new container").hasPrevious(origContainer);
			assertThat(container).as("new container").isLast();
			assertThat(origContainer).as("orig container").hasNext(container);
			assertThat(origContainer.getPreviousVersion()).isFirst();

			assertEquals(1, dummySearchProvider.getStoreEvents().size());
		}

	}

	@Test
	@Override
	public void testUpdateByUUIDWithoutPerm() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Node node = folder("2015");
			role().revokePermissions(node, UPDATE_PERM);
			String uuid = node.getUuid();
			NodeUpdateRequest request = new NodeUpdateRequest();
			SchemaReference reference = new SchemaReference();
			reference.setName("content");
			request.setSchema(reference);
			request.setLanguage("en");

			MeshResponse<NodeResponse> future = client().updateNode(PROJECT_NAME, uuid, request).invoke();
			latchFor(future);
			expectException(future, FORBIDDEN, "error_missing_perm", uuid);
		}
	}

	@Test
	@Override
	public void testUpdateWithBogusUuid() throws GenericRestException, Exception {

		try (NoTx noTx = db.noTx()) {
			NodeUpdateRequest request = new NodeUpdateRequest();
			SchemaReference schemaReference = new SchemaReference();
			schemaReference.setName("folder");
			schemaReference.setUuid(schemaContainer("folder").getUuid());
			request.setSchema(schemaReference);
			request.setLanguage("en");

			NodeParameters parameters = new NodeParameters();
			parameters.setLanguages("en", "de");

			MeshResponse<NodeResponse> future = client().updateNode(PROJECT_NAME, "bogus", request, parameters).invoke();
			latchFor(future);
			expectException(future, NOT_FOUND, "object_not_found_for_uuid", "bogus");
		}
	}

	@Test
	public void testCreateNodeWithExtraField() throws UnknownHostException, InterruptedException {
		try (NoTx noTx = db.noTx()) {
			Node parentNode = folder("news");
			String uuid = parentNode.getUuid();
			assertNotNull(parentNode);
			assertNotNull(parentNode.getUuid());

			NodeCreateRequest request = new NodeCreateRequest();
			request.setSchema(new SchemaReference().setName("content").setUuid(schemaContainer("content").getUuid()));
			request.setLanguage("en");
			request.getFields().put("title", FieldUtil.createStringField("some title"));
			request.getFields().put("extrafield", FieldUtil.createStringField("some extra field value"));
			request.getFields().put("name", FieldUtil.createStringField("some name"));
			request.getFields().put("filename", FieldUtil.createStringField("new-page.html"));
			request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));

			request.setParentNodeUuid(uuid);

			call(() -> client().createNode(PROJECT_NAME, request), BAD_REQUEST, "node_unhandled_fields", "content", "[extrafield]");
		}

	}

	@Test
	public void testCreateNodeWithMissingRequiredField() {
		try (NoTx noTx = db.noTx()) {
			Node parentNode = folder("news");
			String uuid = parentNode.getUuid();
			assertNotNull(parentNode);
			assertNotNull(parentNode.getUuid());

			NodeCreateRequest request = new NodeCreateRequest();
			request.setSchema(new SchemaReference().setName("content").setUuid(schemaContainer("content").getUuid()));
			request.setLanguage("en");
			// non required title field is missing
			// required name field is missing
			request.getFields().put("filename", FieldUtil.createStringField("new-page.html"));
			request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));

			request.setParentNodeUuid(uuid);

			MeshResponse<NodeResponse> future = client().createNode(PROJECT_NAME, request).invoke();
			latchFor(future);
			expectException(future, BAD_REQUEST, "node_error_missing_required_field_value", "name", "content");
			assertNull(future.result());
		}
	}

	@Test
	public void testCreateNodeWithMissingField() throws UnknownHostException, InterruptedException {
		try (NoTx noTx = db.noTx()) {
			Node parentNode = folder("news");
			String uuid = parentNode.getUuid();
			assertNotNull(parentNode);
			assertNotNull(parentNode.getUuid());

			NodeCreateRequest request = new NodeCreateRequest();
			request.setSchema(new SchemaReference().setName("content").setUuid(schemaContainer("content").getUuid()));
			request.setLanguage("en");
			// title field is missing
			request.getFields().put("name", FieldUtil.createStringField("some name"));
			request.getFields().put("filename", FieldUtil.createStringField("new-page.html"));
			request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));

			request.setParentNodeUuid(uuid);

			MeshResponse<NodeResponse> future = client().createNode(PROJECT_NAME, request).invoke();
			latchFor(future);
			assertSuccess(future);
			assertNotNull(future.result());
		}
	}

	@Test
	public void testUpdateNodeWithExtraField2() throws GenericRestException, Exception {
		try (NoTx noTx = db.noTx()) {
			Node node = folder("2015");
			String uuid = node.getUuid();

			NodeUpdateRequest request = new NodeUpdateRequest();
			request.setSchema(new SchemaReference().setName("content"));
			request.setLanguage("en");
			request.setVersion(new VersionReference(null, "0.1"));
			final String newName = "english renamed name";
			final String newDisplayName = "display name changed";

			request.getFields().put("name", FieldUtil.createStringField(newName));

			// Add another field which has not been specified in the content schema
			request.getFields().put("someField", FieldUtil.createStringField(newDisplayName));

			NodeParameters parameters = new NodeParameters();
			parameters.setLanguages("de", "en");
			call(() -> client().updateNode(PROJECT_NAME, uuid, request, parameters), BAD_REQUEST, "node_unhandled_fields", "folder", "[someField]");

			NodeGraphFieldContainer englishContainer = folder("2015").getLatestDraftFieldContainer(english());
			assertNotEquals("The name should not have been changed.", newName, englishContainer.getString("name").getString());
		}
	}

	// Delete

	@Test
	public void testDeleteBaseNode() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Node node = project().getBaseNode();
			String uuid = node.getUuid();

			call(() -> client().deleteNode(PROJECT_NAME, uuid), METHOD_NOT_ALLOWED, "node_basenode_not_deletable");

			Node foundNode = meshRoot().getNodeRoot().findByUuid(uuid);
			assertNotNull("The node should still exist.", foundNode);
		}
	}

	@Test
	@Override
	public void testDeleteByUUID() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Node node = content("concorde");
			String uuid = node.getUuid();
			call(() -> client().deleteNode(PROJECT_NAME, uuid));

			assertElement(meshRoot().getNodeRoot(), uuid, false);
			assertThat(dummySearchProvider).as("Delete Events after node delete. We expect 4 since both languages have draft and publish version.")
					.recordedDeleteEvents(4);
			// SearchQueueBatch batch = searchQueue.take();
			// assertEquals(1, batch.getEntries().size());
			// SearchQueueEntry entry = batch.getEntries().get(0);
			// assertEquals(uuid, entry.getElementUuid());
			// assertEquals(Node.TYPE, entry.getElementType());
			// assertEquals(SearchQueueEntryAction.DELETE_ACTION, entry.getElementAction());
		}
	}

	@Test
	public void testDeleteForRelease() throws Exception {
		try (NoTx noTx = db.noTx()) {
			// 1. get the node
			Node node = content("concorde");
			String uuid = node.getUuid();

			// 2. create new release
			Project project = project();
			Release initialRelease = project.getInitialRelease();
			Release newRelease = project.getReleaseRoot().create("newrelease", user());

			// 3. migrate nodes
			meshDagger.nodeMigrationHandler().migrateNodes(newRelease).await();
			call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParameters().draft().setRelease(initialRelease.getUuid())));
			call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParameters().draft().setRelease(newRelease.getUuid())));

			// 4. delete node in new release
			call(() -> client().deleteNode(PROJECT_NAME, uuid, new VersioningParameters().setRelease(newRelease.getUuid())));

			// 5. Assert
			assertElement(meshRoot().getNodeRoot(), uuid, true);
			node.reload();
			assertThat(node.getGraphFieldContainers(initialRelease, ContainerType.DRAFT)).as("draft containers for initial release").isNotEmpty();
			assertThat(node.getGraphFieldContainers(newRelease, ContainerType.DRAFT)).as("draft containers for new release").isEmpty();
		}
	}

	@Test
	public void testDeletePublishedForRelease() throws Exception {
		try (NoTx noTx = db.noTx()) {
			// 1. get the node
			Node node = content("concorde");
			String uuid = node.getUuid();

			// 2. Publish the node
			node.publish(getMockedInternalActionContext()).await();

			// 3. create new release
			Project project = project();
			Release initialRelease = project.getInitialRelease();
			Release newRelease = project.getReleaseRoot().create("newrelease", user());

			// 4. migrate nodes
			meshDagger.nodeMigrationHandler().migrateNodes(newRelease).await();
			call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParameters().draft().setRelease(initialRelease.getUuid())));
			call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParameters().draft().setRelease(newRelease.getUuid())));

			// 5. delete node in new release
			call(() -> client().deleteNode(PROJECT_NAME, uuid, new VersioningParameters().setRelease(newRelease.getUuid())));

			// 6. Assert
			assertElement(meshRoot().getNodeRoot(), uuid, true);
			node.reload();
			assertThat(node.getGraphFieldContainers(initialRelease, ContainerType.DRAFT)).as("draft containers for initial release").isNotEmpty();
			assertThat(node.getGraphFieldContainers(initialRelease, ContainerType.PUBLISHED)).as("published containers for initial release")
					.isNotEmpty();
			assertThat(node.getGraphFieldContainers(newRelease, ContainerType.DRAFT)).as("draft containers for new release").isEmpty();
			assertThat(node.getGraphFieldContainers(newRelease, ContainerType.PUBLISHED)).as("published containers for new release").isEmpty();
		}
	}

	@Test
	@Override
	public void testDeleteByUUIDWithNoPermission() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Node node = folder("2015");
			String uuid = node.getUuid();
			role().revokePermissions(node, DELETE_PERM);

			MeshResponse<Void> future = client().deleteNode(PROJECT_NAME, uuid).invoke();
			latchFor(future);
			expectException(future, FORBIDDEN, "error_missing_perm", uuid);

			assertNotNull(meshRoot().getNodeRoot().findByUuid(uuid));
		}
	}

	@Test
	public void testConflictByUpdateAdditionalLanguage() {
		try (NoTx noTx = db.noTx()) {
			Node node = folder("2015");
			String nodeUuid = node.getUuid();

			NodeUpdateRequest update = new NodeUpdateRequest();
			update.setLanguage("de");
			update.getFields().put("name", FieldUtil.createStringField("2015"));
			call(() -> client().updateNode(PROJECT_NAME, nodeUuid, update), CONFLICT, "node_conflicting_segmentfield_update", "name", "2015");
			// TODO also assert message properties
		}
	}

}
