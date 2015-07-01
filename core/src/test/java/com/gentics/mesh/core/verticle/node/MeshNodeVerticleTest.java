package com.gentics.mesh.core.verticle.node;

import static com.gentics.mesh.core.data.relationship.Permission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.READ_PERM;
import static com.gentics.mesh.demo.DemoDataProvider.PROJECT_NAME;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import io.vertx.core.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractRestVerticle;
import com.gentics.mesh.core.data.MeshNodeFieldContainer;
import com.gentics.mesh.core.data.node.MeshNode;
import com.gentics.mesh.core.data.service.MeshNodeService;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.verticle.MeshNodeVerticle;
import com.gentics.mesh.error.HttpStatusCodeErrorException;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.AbstractRestVerticleTest;
import com.gentics.mesh.util.BlueprintTransaction;

public class MeshNodeVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private MeshNodeVerticle verticle;

	@Autowired
	private MeshNodeService nodeService;

	@Override
	public AbstractRestVerticle getVerticle() {
		return verticle;
	}

	// Create tests

	@Test
	public void testCreateNodeWithBogusLanguageCode() throws HttpStatusCodeErrorException, Exception {

		NodeCreateRequest request = new NodeCreateRequest();
		SchemaReference schemaReference = new SchemaReference();
		schemaReference.setName("content");
		request.setLanguage("BOGUS");
		//		NodeFieldContainer fields = new NodeFieldContainer();
		//		fields.put("name", getStringField("some name"));
		//		fields.put("filename", getStringField("new-page.html"));
		//		fields.put("content", getStringField("Blessed mealtime again!"));
		//		request.setFields(fields);
		request.setSchema(schemaReference);
		request.setParentNodeUuid(data().getFolder("news").getUuid());

		String response = request(info, POST, "/api/v1/" + PROJECT_NAME + "/nodes", 400, "Bad Request", JsonUtil.toJson(request));
		expectMessageResponse("error_language_not_found", response, "english");
	}

	@Test
	public void testCreateNode() throws Exception {

		MeshNode parentNode = data().getFolder("news");
		assertNotNull(parentNode);
		assertNotNull(parentNode.getUuid());

		NodeCreateRequest request = new NodeCreateRequest();
		SchemaReference schemaReference = new SchemaReference();
		schemaReference.setName("content");
		request.setSchema(schemaReference);

		//		NodeFieldContainer fields = new NodeFieldContainer();
		//		fields.put("name", getStringField("some name"));
		//		fields.put("filename", getStringField("new-page.html"));
		//		fields.put("content", getStringField("Blessed mealtime again!"));
		//		request.setFields(fields);

		request.setParentNodeUuid(parentNode.getUuid());

		String response = request(info, POST, "/api/v1/" + PROJECT_NAME + "/nodes", 200, "OK", JsonUtil.toJson(request));
		test.assertMeshNode(request, JsonUtil.readValue(response, NodeResponse.class));

	}

	@Test
	public void testCreateReadDeleteNode() throws Exception {
		NodeCreateRequest request = new NodeCreateRequest();
		SchemaReference schemaReference = new SchemaReference();
		schemaReference.setName("content");
		request.setSchema(schemaReference);

		//		NodeFieldContainer fields = new NodeFieldContainer();
		//		fields.put("name", getStringField("some name"));
		//		fields.put("filename", getStringField("new-page.html"));
		//		fields.put("content", getStringField("Blessed mealtime again!"));
		//		request.setFields(fields);

		request.setParentNodeUuid(data().getFolder("news").getUuid());

		// Create node
		String response = request(info, POST, "/api/v1/" + PROJECT_NAME + "/nodes", 200, "OK", JsonUtil.toJson(request));
		NodeResponse restNode = JsonUtil.readValue(response, NodeResponse.class);
		test.assertMeshNode(request, restNode);

		MeshNode node = nodeService.findByUUID(restNode.getUuid());
		assertNotNull(node);
		test.assertMeshNode(request, node);

		// Load the node again
		response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/nodes/" + restNode.getUuid(), 200, "OK");
		restNode = JsonUtil.readValue(response, NodeResponse.class);
		test.assertMeshNode(node, restNode);

		// Delete the node
		response = request(info, DELETE, "/api/v1/" + PROJECT_NAME + "/nodes/" + restNode.getUuid(), 200, "OK");
		expectMessageResponse("node_deleted", response, restNode.getUuid());

		assertNull("The node should have been deleted.", node);

	}

	@Test
	public void testCreateNodeWithMissingParentNodeUuid() throws Exception {

		NodeCreateRequest request = new NodeCreateRequest();
		SchemaReference schemaReference = new SchemaReference();
		schemaReference.setName("node");
		request.setSchema(schemaReference);
		//		NodeFieldContainer fields = new NodeFieldContainer();
		//		fields.put("name", getStringField("some name"));
		//		fields.put("filename", getStringField("new-page.html"));
		//		fields.put("content", getStringField("Blessed mealtime again!"));
		//		request.setFields(fields);

		String response = request(info, POST, "/api/v1/" + PROJECT_NAME + "/nodes", 400, "Bad Request", JsonUtil.toJson(request));
		expectMessageResponse("node_missing_parentnode_field", response);

	}

	@Test
	public void testCreateNodeWithMissingPermission() throws Exception {

		// Revoke create perm
		info.getRole().revokePermissions(data().getFolder("news"), CREATE_PERM);

		NodeCreateRequest request = new NodeCreateRequest();
		SchemaReference schemaReference = new SchemaReference();
		schemaReference.setName("node");
		request.setSchema(schemaReference);
		//		NodeFieldContainer fields = new NodeFieldContainer();
		//		fields.put("name", getStringField("some name"));
		//		fields.put("filename", getStringField("new-page.html"));
		//		fields.put("content", getStringField("Blessed mealtime again!"));
		//		request.setFields(fields);

		request.setParentNodeUuid(data().getFolder("news").getUuid());

		String response = request(info, POST, "/api/v1/" + PROJECT_NAME + "/nodes", 403, "Forbidden", JsonUtil.toJson(request));
		expectMessageResponse("error_missing_perm", response, data().getFolder("news").getUuid());
	}

	// Read tests

	@Test
	public void testReadNodesDefaultPaging() throws Exception {
		// Test default paging parameters
		String response = request(info, HttpMethod.GET, "/api/v1/" + PROJECT_NAME + "/nodes", 200, "OK");
		NodeListResponse restResponse = JsonUtil.readValue(response, NodeListResponse.class);
		assertNotNull(restResponse);
		assertEquals(25, restResponse.getMetainfo().getPerPage());
		assertEquals(1, restResponse.getMetainfo().getCurrentPage());
		assertEquals(25, restResponse.getData().size());
	}

	@Test
	public void testReadNodes() throws Exception {
		MeshNode parentNode = data().getFolder("2015");
		// Don't grant permissions to the no perm node. We want to make sure that this one will not be listed.
		MeshNode noPermNode = parentNode.create();
		noPermNode.setCreator(info.getUser());
		assertNotNull(noPermNode.getUuid());

		int perPage = 11;
		String response = request(info, HttpMethod.GET, "/api/v1/" + PROJECT_NAME + "/nodes/?per_page=" + perPage + "&page=" + 3, 200, "OK");
		NodeListResponse restResponse = JsonUtil.readValue(response, NodeListResponse.class);
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
			response = request(info, HttpMethod.GET, "/api/v1/" + PROJECT_NAME + "/nodes/?per_page=" + perPage + "&page=" + page, 200, "OK");
			restResponse = JsonUtil.readValue(response, NodeListResponse.class);
			allNodes.addAll(restResponse.getData());
		}
		assertEquals("Somehow not all users were loaded when loading all pages.", totalNodes, allNodes.size());

		// Verify that the no_perm_node is not part of the response
		final String noPermNodeUUID = noPermNode.getUuid();
		List<NodeResponse> filteredUserList = allNodes.parallelStream().filter(restNode -> restNode.getUuid().equals(noPermNodeUUID))
				.collect(Collectors.toList());
		assertTrue("The no perm node should not be part of the list since no permissions were added.", filteredUserList.size() == 0);

		response = request(info, HttpMethod.GET, "/api/v1/" + PROJECT_NAME + "/nodes/?per_page=25&page=-1", 400, "Bad Request");
		expectMessageResponse("error_invalid_paging_parameters", response);
		response = request(info, HttpMethod.GET, "/api/v1/" + PROJECT_NAME + "/nodes/?per_page=25&page=0", 400, "Bad Request");
		expectMessageResponse("error_invalid_paging_parameters", response);
		response = request(info, HttpMethod.GET, "/api/v1/" + PROJECT_NAME + "/nodes/?per_page=0&page=1", 400, "Bad Request");
		expectMessageResponse("error_invalid_paging_parameters", response);
		response = request(info, HttpMethod.GET, "/api/v1/" + PROJECT_NAME + "/nodes/?per_page=-1&page=1", 400, "Bad Request");
		expectMessageResponse("error_invalid_paging_parameters", response);

		response = request(info, HttpMethod.GET, "/api/v1/" + PROJECT_NAME + "/nodes/?per_page=25&page=4242", 200, "OK");
		NodeListResponse list = JsonUtil.readValue(response, NodeListResponse.class);
		assertEquals(4242, list.getMetainfo().getCurrentPage());
		assertEquals(0, list.getData().size());
		assertEquals(25, list.getMetainfo().getPerPage());
		assertEquals(3, list.getMetainfo().getPageCount());
		assertEquals(data().getNodeCount(), list.getMetainfo().getTotalCount());

	}

	@Test
	public void testReadNodesWithoutPermissions() throws Exception {

		// TODO add node that has no perms and check the response
		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/nodes/", 200, "OK");
		NodeListResponse restResponse = JsonUtil.readValue(response, NodeListResponse.class);

		int nElements = restResponse.getData().size();
		assertEquals("The amount of elements in the list did not match the expected count", 25, nElements);
		assertEquals(1, restResponse.getMetainfo().getCurrentPage());
		assertEquals(3, restResponse.getMetainfo().getPageCount());
		assertEquals(25, restResponse.getMetainfo().getPerPage());
		assertEquals(data().getNodeCount(), restResponse.getMetainfo().getTotalCount());
	}

	@Test
	public void testReadNodeByUUID() throws Exception {
		MeshNode node = data().getFolder("2015");
		assertNotNull(node);
		assertNotNull(node.getUuid());

		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/nodes/" + node.getUuid(), 200, "OK");
		test.assertMeshNode(node, JsonUtil.readValue(response, NodeResponse.class));
	}

	@Test
	public void testReadNodeByUUIDSingleLanguage() throws Exception {
		MeshNode node = data().getFolder("products");

		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/nodes/" + node.getUuid() + "?lang=de", 200, "OK");
		NodeResponse restNode = JsonUtil.readValue(response, NodeResponse.class);
		test.assertMeshNode(node, restNode);

		//		assertNull(restNode.getProperties());
		//		assertEquals("Produkte", restNode.getProperty("name"));
	}

	@Test
	public void testReadNodeWithBogusLanguageCode() throws Exception {

		MeshNode node = data().getFolder("2015");
		assertNotNull(node);
		assertNotNull(node.getUuid());

		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/nodes/" + node.getUuid() + "?lang=blabla,edgsdg", 400, "Bad Request");
		expectMessageResponse("error_language_not_found", response, "blabla");

	}

	@Test
	public void testReadNodeByUUIDWithoutPermission() throws Exception {
		MeshNode node = data().getFolder("2015");
		//		try (Transaction tx = graphDb.beginTx()) {
		info.getRole().revokePermissions(node, READ_PERM);
		//			tx.success();
		//		}
		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/nodes/" + node.getUuid(), 403, "Forbidden");
		expectMessageResponse("error_missing_perm", response, node.getUuid());
	}

	@Test
	public void testReadNodeByBogusUUID() throws Exception {
		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/nodes/bogusUUID", 404, "Not Found");
		expectMessageResponse("object_not_found_for_uuid", response, "bogusUUID");
	}

	@Test
	public void testReadNodeByInvalidUUID() throws Exception {
		String uuid = "dde8ba06bb7211e4897631a9ce2772f5";
		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/nodes/" + uuid, 404, "Not Found");
		expectMessageResponse("object_not_found_for_uuid", response, uuid);
	}

	// Update

	@Test
	public void testUpdateNode() throws HttpStatusCodeErrorException, Exception {
		NodeUpdateRequest request = new NodeUpdateRequest();
		SchemaReference schemaReference = new SchemaReference();
		schemaReference.setName("content");
		request.setSchema(schemaReference);
		final String newFilename = "new-name.html";
		final String newName = "english renamed name";
		final String newContent = "english renamed content!";

		//		NodeFieldContainer fields = new NodeFieldContainer();
		//		fields.put("name", getStringField(newName));
		//		fields.put("filename", getStringField(newFilename));
		//		fields.put("content", getStringField(newContent));
		//		request.setFields(fields);

		String response = request(info, PUT, "/api/v1/" + PROJECT_NAME + "/nodes/" + data().getFolder("2015").getUuid() + "?lang=de,en", 200, "OK",
				JsonUtil.toJson(request));
		NodeResponse restNode = JsonUtil.readValue(response, NodeResponse.class);
		//		assertEquals(newFilename, restNode.getProperty("filename"));
		//		assertEquals(newName, restNode.getProperty("name"));
		//		assertEquals(newContent, restNode.getProperty("content"));
		// TODO verify that the node got updated

	}

	@Test
	public void testUpdateNodeWithExtraJson() throws HttpStatusCodeErrorException, Exception {
		NodeUpdateRequest request = new NodeUpdateRequest();
		SchemaReference schemaReference = new SchemaReference();
		schemaReference.setName("content");
		request.setSchema(schemaReference);
		final String newFilename = "new-name.html";
		final String newName = "english renamed name";
		final String newContent = "english renamed content!";

		//		NodeFieldContainer fields = new NodeFieldContainer();
		//		fields.put("name", getStringField(newName));
		//		fields.put("filename", getStringField(newFilename));
		//		fields.put("content", getStringField(newContent));
		//		request.setFields(fields);

		MeshNode node = data().getFolder("2015");
		String json = JsonUtil.toJson(request);
		String response = request(info, PUT, "/api/v1/" + PROJECT_NAME + "/nodes/" + node.getUuid() + "?lang=de,en", 200, "OK", json);
		NodeResponse restNode = JsonUtil.readValue(response, NodeResponse.class);
		//		assertEquals(newFilename, restNode.getProperty("displayName"));
		//		assertEquals(newName, restNode.getProperty("name"));
		//		assertEquals(newContent, restNode.getProperty("content"));

		// TODO Reload and update
		MeshNodeFieldContainer englishContainer = node.getOrCreateFieldContainer(data().getEnglish());
		assertEquals(newFilename, englishContainer.getString("displayName").getString());
		assertEquals(newName, englishContainer.getString("name").getString());
		assertEquals(newContent, englishContainer.getString("content").getString());

	}

	// Delete

	@Test
	public void testDeleteNode() throws Exception {

		MeshNode node = data().getFolder("2015");
		String response = request(info, DELETE, "/api/v1/" + PROJECT_NAME + "/nodes/" + node.getUuid(), 200, "OK");
		expectMessageResponse("node_deleted", response, node.getUuid());
		assertNull(nodeService.findByUUID(node.getUuid()));
	}

	@Test
	public void testDeleteNodeWithNoPerm() throws Exception {

		String uuid = data().getFolder("2015").getUuid();
		try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
			MeshNode node = data().getFolder("2015");
			info.getRole().revokePermissions(node, DELETE_PERM);
			tx.success();
		}

		String response = request(info, DELETE, "/api/v1/" + PROJECT_NAME + "/nodes/" + uuid, 403, "Forbidden");
		expectMessageResponse("error_missing_perm", response, uuid);

		assertNotNull(nodeService.findByUUID(uuid));
	}

	private static StringField getStringField(String string) {
		StringField field = new StringFieldImpl();
		return field;
	}
}
