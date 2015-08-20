package com.gentics.mesh.core.verticle.node;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.demo.DemoDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.failingLatch;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.Collectors;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeRequestParameters;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.verticle.project.ProjectNodeVerticle;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.test.AbstractRestVerticleTest;
import com.gentics.mesh.util.FieldUtil;

import io.vertx.core.Future;

public class ProjectNodeVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private ProjectNodeVerticle verticle;

	@Override
	public AbstractWebVerticle getVerticle() {
		return verticle;
	}

	// Create tests

	@Test
	public void testCreateNodeWithNoLanguageCode() {
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
		Future<NodeResponse> future = getClient().createNode(PROJECT_NAME, request);
		latchFor(future);
		expectException(future, BAD_REQUEST, "node_no_languagecode_specified");
	}

	@Test
	public void testCreateNodeWithBogusLanguageCode() throws HttpStatusCodeErrorException, Exception {

		NodeCreateRequest request = new NodeCreateRequest();
		SchemaReference schemaReference = new SchemaReference();
		schemaReference.setName("content");
		schemaReference.setUuid(schemaContainer("content").getUuid());
		request.setLanguage("BOGUS");
		request.getFields().put("name", FieldUtil.createStringField("some name"));
		request.getFields().put("filename", FieldUtil.createStringField("new-page.html"));
		request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));

		request.setSchema(schemaReference);
		request.setParentNodeUuid(folder("news").getUuid());
		Future<NodeResponse> future = getClient().createNode(PROJECT_NAME, request);
		latchFor(future);
		expectException(future, BAD_REQUEST, "node_no_language_found", "BOGUS");
	}

	@Test
	public void testCreateNodeInBaseNode() {
		NodeCreateRequest request = new NodeCreateRequest();
		request.setSchema(new SchemaReference("content", schemaContainer("content").getUuid()));
		request.setLanguage("en");
		request.getFields().put("title", FieldUtil.createStringField("some title"));
		request.getFields().put("name", FieldUtil.createStringField("some name"));
		request.getFields().put("filename", FieldUtil.createStringField("new-page.html"));
		request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));

		try (Trx tx = new Trx(db)) {
			request.setParentNodeUuid(project().getBaseNode().getUuid());
		}

		Future<NodeResponse> future = getClient().createNode(PROJECT_NAME, request);
		latchFor(future);
		assertSuccess(future);
		NodeResponse restNode = future.result();
		test.assertMeshNode(request, restNode);
	}

	@Test
	public void testCreateNode() throws Exception {
		String uuid;
		try (Trx tx = new Trx(db)) {
			Node parentNode = folder("news");
			uuid = parentNode.getUuid();
			assertNotNull(parentNode);
			assertNotNull(parentNode.getUuid());
		}

		NodeCreateRequest request = new NodeCreateRequest();
		request.setSchema(new SchemaReference("content", schemaContainer("content").getUuid()));
		request.setLanguage("en");
		request.getFields().put("title", FieldUtil.createStringField("some title"));
		request.getFields().put("name", FieldUtil.createStringField("some name"));
		request.getFields().put("filename", FieldUtil.createStringField("new-page.html"));
		request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
		request.setPublished(true);
		request.setParentNodeUuid(uuid);

		Future<NodeResponse> future = getClient().createNode(PROJECT_NAME, request);
		latchFor(future);
		assertSuccess(future);
		NodeResponse restNode = future.result();
		test.assertMeshNode(request, restNode);

		try (Trx tx = new Trx(db)) {
			SearchQueue searchQueue = meshRoot().getSearchQueue();
			assertEquals("We created the node. A search queue entry should have been created.", 1, searchQueue.getSize());
			SearchQueueEntry entry = searchQueue.take();
			assertEquals(restNode.getUuid(), entry.getElementUuid());
			assertEquals(Node.TYPE, entry.getElementType());
			assertEquals(SearchQueueEntryAction.CREATE_ACTION, entry.getAction());
		}

	}

	@Test
	public void testCreateReadDeleteNode() throws Exception {
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

		// Create node
		NodeRequestParameters parameters = new NodeRequestParameters();
		parameters.setLanguages("de");
		Future<NodeResponse> future = getClient().createNode(PROJECT_NAME, request, parameters);
		latchFor(future);
		assertSuccess(future);
		NodeResponse restNode = future.result();
		test.assertMeshNode(request, restNode);

		try (Trx tx = new Trx(db)) {
			CountDownLatch latch = new CountDownLatch(2);
			meshRoot().getNodeRoot().findByUuid(restNode.getUuid(), rh -> {
				Node node = rh.result();
				assertNotNull(node);
				test.assertMeshNode(request, node);
				// Load the node again
				Future<NodeResponse> future2 = getClient().findNodeByUuid(PROJECT_NAME, restNode.getUuid(), parameters);
				latchFor(future2);
				assertSuccess(future2);
				NodeResponse restNode2 = future2.result();
				test.assertMeshNode(node, restNode2);

				// Delete the node
				Future<GenericMessageResponse> deleteFut = getClient().deleteNode(PROJECT_NAME, restNode2.getUuid());
				latchFor(deleteFut);
				assertSuccess(deleteFut);
				expectMessageResponse("node_deleted", deleteFut, restNode2.getUuid());
				meshRoot().getNodeRoot().findByUuid(restNode2.getUuid(), rh2 -> {
					assertNull("The node should have been deleted.", rh2.result());
					latch.countDown();
				});
				latch.countDown();
			});
			failingLatch(latch);
		}

	}

	@Test
	public void testCreateNodeWithMissingParentNodeUuid() throws Exception {

		NodeCreateRequest request = new NodeCreateRequest();
		SchemaReference schemaReference = new SchemaReference();
		schemaReference.setName("node");
		request.setSchema(schemaReference);
		request.getFields().put("name", FieldUtil.createStringField("some name"));
		request.getFields().put("filename", FieldUtil.createStringField("new-page.html"));
		request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
		request.setSchema(new SchemaReference("content", schemaContainer("content").getUuid()));

		Future<NodeResponse> future = getClient().createNode(PROJECT_NAME, request);
		latchFor(future);
		expectException(future, BAD_REQUEST, "node_missing_parentnode_field");

	}

	@Test
	public void testCreateNodeWithMissingPermission() throws Exception {

		// Revoke create perm
		String uuid;
		try (Trx tx = new Trx(db)) {
			Node node = folder("news");
			uuid = node.getUuid();
			role().revokePermissions(node, CREATE_PERM);
			tx.success();
		}

		NodeCreateRequest request = new NodeCreateRequest();
		SchemaReference schemaReference = new SchemaReference("content", schemaContainer("content").getUuid());
		request.setSchema(schemaReference);
		request.getFields().put("name", FieldUtil.createStringField("some name"));
		request.getFields().put("filename", FieldUtil.createStringField("new-page.html"));
		request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
		request.setSchema(new SchemaReference("content", schemaContainer("content").getUuid()));
		request.setLanguage("en");
		request.setParentNodeUuid(uuid);

		Future<NodeResponse> future = getClient().createNode(PROJECT_NAME, request);
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", uuid);
	}

	// Read tests

	/**
	 * Test default paging parameters
	 * 
	 * @throws Exception
	 */
	@Test
	public void testReadNodesDefaultPaging() throws Exception {

		Future<NodeListResponse> future = getClient().findNodes(PROJECT_NAME);
		latchFor(future);
		assertSuccess(future);

		NodeListResponse restResponse = future.result();
		assertNotNull(restResponse);
		assertEquals(25, restResponse.getMetainfo().getPerPage());
		assertEquals(1, restResponse.getMetainfo().getCurrentPage());
		assertEquals(25, restResponse.getData().size());
	}

	@Test
	public void testReadNodes() throws Exception {
		final String noPermNodeUUID;

		try (Trx tx = new Trx(db)) {
			Node parentNode = folder("2015");
			// Don't grant permissions to the no perm node. We want to make sure that this one will not be listed.
			Node noPermNode = parentNode.create(user(), schemaContainer("content"), project());
			noPermNode.setCreator(user());
			noPermNodeUUID = noPermNode.getUuid();
			assertNotNull(noPermNode.getUuid());
		}
		int perPage = 11;
		Future<NodeListResponse> future = getClient().findNodes(PROJECT_NAME, new PagingInfo(3, perPage));
		latchFor(future);
		assertSuccess(future);
		NodeListResponse restResponse = future.result();
		assertEquals(perPage, restResponse.getData().size());

		// Extra Nodes + permitted node
		int totalNodes = data().getNodeCount();
		int totalPages = (int) Math.ceil(totalNodes / (double) perPage);
		assertEquals("The response did not contain the correct amount of items", perPage, restResponse.getData().size());
		assertEquals(3, restResponse.getMetainfo().getCurrentPage());
		assertEquals(totalNodes, restResponse.getMetainfo().getTotalCount());
		assertEquals(totalPages, restResponse.getMetainfo().getPageCount());
		assertEquals(perPage, restResponse.getMetainfo().getPerPage());

		List<NodeResponse> allNodes = new ArrayList<>();
		for (int page = 1; page <= totalPages; page++) {
			Future<NodeListResponse> pageFuture = getClient().findNodes(PROJECT_NAME, new PagingInfo(page, perPage));
			latchFor(pageFuture);
			assertSuccess(pageFuture);
			restResponse = pageFuture.result();
			allNodes.addAll(restResponse.getData());
		}
		assertEquals("Somehow not all users were loaded when loading all pages.", totalNodes, allNodes.size());

		// Verify that the no_perm_node is not part of the response
		List<NodeResponse> filteredUserList = allNodes.parallelStream().filter(restNode -> restNode.getUuid().equals(noPermNodeUUID))
				.collect(Collectors.toList());
		assertTrue("The no perm node should not be part of the list since no permissions were added.", filteredUserList.size() == 0);

		Future<NodeListResponse> pageFuture = getClient().findNodes(PROJECT_NAME, new PagingInfo(-1, 25));
		latchFor(pageFuture);
		expectException(pageFuture, BAD_REQUEST, "error_invalid_paging_parameters");

		pageFuture = getClient().findNodes(PROJECT_NAME, new PagingInfo(0, 25));
		latchFor(pageFuture);
		expectException(pageFuture, BAD_REQUEST, "error_invalid_paging_parameters");

		pageFuture = getClient().findNodes(PROJECT_NAME, new PagingInfo(1, 0));
		latchFor(pageFuture);
		expectException(pageFuture, BAD_REQUEST, "error_invalid_paging_parameters");

		pageFuture = getClient().findNodes(PROJECT_NAME, new PagingInfo(1, -1));
		latchFor(pageFuture);
		expectException(pageFuture, BAD_REQUEST, "error_invalid_paging_parameters");

		pageFuture = getClient().findNodes(PROJECT_NAME, new PagingInfo(4242, 25));
		latchFor(pageFuture);
		assertSuccess(pageFuture);
		NodeListResponse list = pageFuture.result();
		assertEquals(4242, list.getMetainfo().getCurrentPage());
		assertEquals(0, list.getData().size());
		assertEquals(25, list.getMetainfo().getPerPage());
		assertEquals(3, list.getMetainfo().getPageCount());
		assertEquals(data().getNodeCount(), list.getMetainfo().getTotalCount());

	}

	@Test
	public void testReadNodesWithoutPermissions() throws Exception {

		// TODO add node that has no perms and check the response
		Future<NodeListResponse> future = getClient().findNodes(PROJECT_NAME, new PagingInfo());
		latchFor(future);
		assertSuccess(future);
		NodeListResponse restResponse = future.result();

		int nElements = restResponse.getData().size();
		assertEquals("The amount of elements in the list did not match the expected count", 25, nElements);
		assertEquals(1, restResponse.getMetainfo().getCurrentPage());
		assertEquals(3, restResponse.getMetainfo().getPageCount());
		assertEquals(25, restResponse.getMetainfo().getPerPage());
		assertEquals(data().getNodeCount(), restResponse.getMetainfo().getTotalCount());
	}

	@Test
	@Ignore("Disabled until custom 404 handler has been added")
	public void testReadNodeWithBogusProject() {
		Future<NodeResponse> future = getClient().findNodeByUuid("BOGUS", "someUuuid");
		latchFor(future);
		expectException(future, BAD_REQUEST, "project_not_found", "BOGUS");
	}

	@Test
	public void testReadNodeByUUIDMultithreaded() throws InterruptedException {
		int nJobs = 5;
		CyclicBarrier barrier = new CyclicBarrier(nJobs);
		Trx.enableDebug();
		Trx.setBarrier(barrier);
		Set<Future<NodeResponse>> set = new HashSet<>();
		for (int i = 0; i < nJobs; i++) {
			set.add(getClient().findNodeByUuid(PROJECT_NAME, folder("2015").getUuid()));
		}
		for (Future<NodeResponse> future : set) {
			latchFor(future);
			assertSuccess(future);
		}
		Trx.disableDebug();

	}

	@Test
	public void testReadNodeByUUID() throws Exception {

		getClient().getClientSchemaStorage().addSchema(schemaContainer("folder").getSchema());

		String uuid;
		try (Trx tx = new Trx(db)) {
			Node node = folder("2015");
			uuid = node.getUuid();
			assertNotNull(node);
			assertNotNull(node.getUuid());
		}

		Future<NodeResponse> future = getClient().findNodeByUuid(PROJECT_NAME, uuid);
		latchFor(future);
		assertSuccess(future);
		try (Trx tx = new Trx(db)) {
			test.assertMeshNode(folder("2015"), future.result());
		}
		NodeResponse response = future.result();
		assertEquals("name", response.getDisplayField());
		assertNotNull(response.getParentNode());
		try (Trx tx = new Trx(db)) {
			assertEquals(folder("2015").getParentNode().getUuid(), response.getParentNode().getUuid());
		}
		assertEquals("News", response.getParentNode().getDisplayName());
		assertEquals("en", response.getLanguage());
	}

	@Test
	public void testReadNodeByUUIDSingleLanguage() throws Exception {
		getClient().getClientSchemaStorage().addSchema(schemaContainer("folder").getSchema());

		String uuid;
		try (Trx tx = new Trx(db)) {
			Node node = folder("products");
			uuid = node.getUuid();
		}

		NodeRequestParameters parameters = new NodeRequestParameters();
		parameters.setLanguages("de");
		Future<NodeResponse> future = getClient().findNodeByUuid(PROJECT_NAME, uuid, parameters);
		latchFor(future);
		assertSuccess(future);
		NodeResponse restNode = future.result();
		try (Trx tx = new Trx(db)) {
			test.assertMeshNode(folder("products"), restNode);
		}

		StringField field = restNode.getField("name");
		String nameText = field.getString();
		assertEquals("Produkte", nameText);
	}

	@Test
	public void testReadNodeWithBogusLanguageCode() throws Exception {

		String uuid;
		try (Trx tx = new Trx(db)) {
			Node node = folder("2015");
			uuid = node.getUuid();
			assertNotNull(node);
			assertNotNull(node.getUuid());
		}

		NodeRequestParameters parameters = new NodeRequestParameters();
		parameters.setLanguages("blabla", "edgsdg");

		Future<NodeResponse> future = getClient().findNodeByUuid(PROJECT_NAME, uuid, parameters);
		latchFor(future);
		expectException(future, BAD_REQUEST, "error_language_not_found", "blabla");

	}

	@Test
	public void testReadNodeByUUIDWithoutPermission() throws Exception {
		String uuid;
		try (Trx tx = new Trx(db)) {
			Node node = folder("2015");
			uuid = node.getUuid();
			role().revokePermissions(node, READ_PERM);
			tx.success();
		}
		Future<NodeResponse> future = getClient().findNodeByUuid(PROJECT_NAME, uuid);
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", uuid);
	}

	@Test
	public void testReadNodeByBogusUUID() throws Exception {

		Future<NodeResponse> future = getClient().findNodeByUuid(PROJECT_NAME, "bogusUUID");
		latchFor(future);
		expectException(future, NOT_FOUND, "object_not_found_for_uuid", "bogusUUID");
	}

	@Test
	public void testReadNodeByInvalidUUID() throws Exception {
		String uuid = "dde8ba06bb7211e4897631a9ce2772f5";

		Future<NodeResponse> future = getClient().findNodeByUuid(PROJECT_NAME, uuid);
		latchFor(future);
		expectException(future, NOT_FOUND, "object_not_found_for_uuid", uuid);

	}

	// Update

	@Test
	public void testUpdateNode() throws HttpStatusCodeErrorException, Exception {
		String uuid;
		try (Trx tx = new Trx(db)) {
			Node node = folder("2015");
			uuid = node.getUuid();
			assertEquals("2015", node.getFieldContainer(english()).getString("name").getString());
		}
		NodeUpdateRequest request = new NodeUpdateRequest();
		SchemaReference schemaReference = new SchemaReference();
		schemaReference.setName("folder");
		schemaReference.setUuid(schemaContainer("folder").getUuid());
		request.setSchema(schemaReference);
		request.setLanguage("en");
		request.setPublished(true);

		final String newName = "english renamed name";
		request.getFields().put("name", FieldUtil.createStringField(newName));

		NodeRequestParameters parameters = new NodeRequestParameters();
		parameters.setLanguages("de", "en");

		Future<NodeResponse> future = getClient().updateNode(PROJECT_NAME, uuid, request, parameters);
		latchFor(future);
		assertSuccess(future);
		NodeResponse restNode = future.result();
		assertNotNull(restNode);
		assertTrue(restNode.isPublished());
		try (Trx tx = new Trx(db)) {
			Node node = folder("2015");
			assertEquals(newName, node.getFieldContainer(english()).getString("name").getString());
		}
		StringField field = restNode.getField("name");
		assertEquals(newName, field.getString());

		try (Trx tx = new Trx(db)) {
			SearchQueue searchQueue = meshRoot().getSearchQueue();
			assertEquals("We updated the node. A search queue entry should have been created.", 1, searchQueue.getSize());
			SearchQueueEntry entry = searchQueue.take();
			assertEquals(restNode.getUuid(), entry.getElementUuid());
			assertEquals(Node.TYPE, entry.getElementType());
			assertEquals(SearchQueueEntryAction.UPDATE_ACTION, entry.getAction());
		}

	}

	@Test
	public void testUpdateNodeWithExtraField() throws UnknownHostException, InterruptedException {
		String uuid;
		try (Trx tx = new Trx(db)) {
			Node parentNode = folder("news");
			uuid = parentNode.getUuid();
			assertNotNull(parentNode);
			assertNotNull(parentNode.getUuid());
		}

		NodeCreateRequest request = new NodeCreateRequest();
		request.setSchema(new SchemaReference("content", schemaContainer("content").getUuid()));
		request.setLanguage("en");
		request.getFields().put("title", FieldUtil.createStringField("some title"));
		request.getFields().put("extrafield", FieldUtil.createStringField("some extra field value"));
		request.getFields().put("name", FieldUtil.createStringField("some name"));
		request.getFields().put("filename", FieldUtil.createStringField("new-page.html"));
		request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));

		request.setParentNodeUuid(uuid);

		Future<NodeResponse> future = getClient().createNode(PROJECT_NAME, request);
		latchFor(future);
		expectMessage(future, BAD_REQUEST,
				"Can't handle field {extrafield} The schema {content} does not specify this key. (through reference chain: com.gentics.mesh.core.rest.node.NodeCreateRequest[\"fields\"])");
		assertNull(future.result());

	}

	@Test
	public void testCreateNodeWithMissingRequiredField() {
		String uuid;
		try (Trx tx = new Trx(db)) {
			Node parentNode = folder("news");
			uuid = parentNode.getUuid();
			assertNotNull(parentNode);
			assertNotNull(parentNode.getUuid());
		}

		NodeCreateRequest request = new NodeCreateRequest();
		request.setSchema(new SchemaReference("content", schemaContainer("content").getUuid()));
		request.setLanguage("en");
		// non required title field is missing
		// required name field is missing
		request.getFields().put("filename", FieldUtil.createStringField("new-page.html"));
		request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));

		request.setParentNodeUuid(uuid);

		Future<NodeResponse> future = getClient().createNode(PROJECT_NAME, request);
		latchFor(future);
		expectException(future, BAD_REQUEST, "Could not find value for required schema field with key {name}");
		assertNull(future.result());

	}

	@Test
	public void testCreateNodeWithMissingField() throws UnknownHostException, InterruptedException {
		String uuid;
		try (Trx tx = new Trx(db)) {
			Node parentNode = folder("news");
			uuid = parentNode.getUuid();
			assertNotNull(parentNode);
			assertNotNull(parentNode.getUuid());
		}

		NodeCreateRequest request = new NodeCreateRequest();
		request.setSchema(new SchemaReference("content", schemaContainer("content").getUuid()));
		request.setLanguage("en");
		// title field is missing
		request.getFields().put("name", FieldUtil.createStringField("some name"));
		request.getFields().put("filename", FieldUtil.createStringField("new-page.html"));
		request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));

		request.setParentNodeUuid(uuid);

		Future<NodeResponse> future = getClient().createNode(PROJECT_NAME, request);
		latchFor(future);
		assertSuccess(future);
		assertNotNull(future.result());
	}

	@Test
	public void testUpdateNodeWithExtraField2() throws HttpStatusCodeErrorException, Exception {
		String uuid;
		try (Trx tx = new Trx(db)) {
			Node node = folder("2015");
			uuid = node.getUuid();
		}
		NodeUpdateRequest request = new NodeUpdateRequest();
		SchemaReference schemaReference = new SchemaReference();
		schemaReference.setName("content");
		request.setSchema(schemaReference);
		request.setLanguage("en");
		final String newName = "english renamed name";
		final String newDisplayName = "display name changed";

		request.getFields().put("name", FieldUtil.createStringField(newName));
		request.getFields().put("displayName", FieldUtil.createStringField(newDisplayName));

		NodeRequestParameters parameters = new NodeRequestParameters();
		parameters.setLanguages("de", "en");
		Future<NodeResponse> future = getClient().updateNode(PROJECT_NAME, uuid, request, parameters);
		latchFor(future);
		expectMessage(future, BAD_REQUEST,
				"Can't handle field {displayName} The schema {content} does not specify this key. (through reference chain: com.gentics.mesh.core.rest.node.NodeUpdateRequest[\"fields\"])");

		assertNull(future.result());

		try (Trx tx = new Trx(db)) {
			NodeFieldContainer englishContainer = folder("2015").getOrCreateFieldContainer(english());
			assertNotEquals(newName, englishContainer.getString("name").getString());
		}

	}

	// Delete

	@Test
	public void testDeleteBaseNode() throws Exception {
		String uuid;
		try (Trx tx = new Trx(db)) {
			Node node = project().getBaseNode();
			uuid = node.getUuid();
		}

		Future<GenericMessageResponse> future = getClient().deleteNode(PROJECT_NAME, uuid);
		latchFor(future);
		expectException(future, METHOD_NOT_ALLOWED, "node_basenode_not_deletable");

		try (Trx tx = new Trx(db)) {
			CountDownLatch latch = new CountDownLatch(1);
			meshRoot().getNodeRoot().findByUuid(uuid, rh -> {
				assertNotNull("The node should still exist.", rh.result());
				latch.countDown();
			});
			failingLatch(latch);
		}
	}

	@Test
	public void testDeleteNode() throws Exception {

		Node node = folder("2015");
		String uuid = node.getUuid();
		Future<GenericMessageResponse> future = getClient().deleteNode(PROJECT_NAME, uuid);
		latchFor(future);
		assertSuccess(future);

		expectMessageResponse("node_deleted", future, uuid);

		try (Trx tx = new Trx(db)) {
			CountDownLatch latch = new CountDownLatch(1);
			meshRoot().getNodeRoot().findByUuid(uuid, rh -> {
				assertNull(rh.result());
				latch.countDown();
			});
			failingLatch(latch);
		}

		try (Trx tx = new Trx(db)) {
			SearchQueue searchQueue = meshRoot().getSearchQueue();
			assertEquals("We deleted the item. A search queue entry should have been created.", 1, searchQueue.getSize());
			SearchQueueEntry entry = searchQueue.take();
			assertEquals(uuid, entry.getElementUuid());
			assertEquals(Node.TYPE, entry.getElementType());
			assertEquals(SearchQueueEntryAction.DELETE_ACTION, entry.getAction());
		}
	}

	@Test
	public void testDeleteNodeWithNoPerm() throws Exception {
		String uuid;
		try (Trx tx = new Trx(db)) {
			Node node = folder("2015");
			uuid = node.getUuid();
			role().revokePermissions(node, DELETE_PERM);
			tx.success();
		}

		Future<GenericMessageResponse> future = getClient().deleteNode(PROJECT_NAME, uuid);
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", uuid);

		try (Trx tx = new Trx(db)) {
			CountDownLatch latch = new CountDownLatch(1);
			meshRoot().getNodeRoot().findByUuid(uuid, rh -> {
				assertNotNull(rh.result());
				latch.countDown();
			});
			failingLatch(latch);
		}
	}
}
