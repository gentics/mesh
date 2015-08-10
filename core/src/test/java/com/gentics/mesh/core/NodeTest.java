package com.gentics.mesh.core;

import static com.gentics.mesh.util.VerticleHelper.getUser;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.data.FieldContainer;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.Permission;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.AbstractBasicObjectTest;
import com.gentics.mesh.util.InvalidArgumentException;

import io.vertx.ext.web.RoutingContext;

public class NodeTest extends AbstractBasicObjectTest {

	@Autowired
	private ServerSchemaStorage schemaStorage;

	/**
	 * Test linking two contents
	 */
	@Test
	public void testPageLinks() {
		Node folder = folder("2015");
		Node node = folder.create(user(), getSchemaContainer(), project());
		Node node2 = folder.create(user(), getSchemaContainer(), project());

		NodeFieldContainer englishContainer = node2.getOrCreateFieldContainer(english());
		englishContainer.createString("content").setString("english content");
		englishContainer.createString("name").setString("english.html");

		NodeFieldContainer englishContainer2 = node.getOrCreateFieldContainer(german());
		englishContainer2.createString("content").setString("english2 content");
		englishContainer2.createString("name").setString("english2.html");
		node.createLink(node2);

		// TODO verify that link relation has been created
		// TODO render content and resolve links
	}

	@Test
	public void testMeshNodeStructure() {
		Node newsNode = content("news overview");
		assertNotNull(newsNode);
		Node newSubNode;
		newSubNode = newsNode.create(user(), getSchemaContainer(), project());

		assertEquals(1, newsNode.getChildren().size());
		Node firstChild = newsNode.getChildren().iterator().next();
		assertEquals(newSubNode.getUuid(), firstChild.getUuid());
	}

	@Test
	public void testTaggingOfMeshNode() {
		Node newsNode = content("news overview");
		assertNotNull(newsNode);

		Tag carTag = tag("car");
		assertNotNull(carTag);

		newsNode.addTag(carTag);

		assertEquals(1, newsNode.getTags().size());
		Tag firstTag = newsNode.getTags().iterator().next();
		assertEquals(carTag.getUuid(), firstTag.getUuid());
	}

	@Test
	@Override
	public void testFindAll() throws InvalidArgumentException {

		List<String> languageTags = new ArrayList<>();
		languageTags.add("de");

		RoutingContext rc = getMockedRoutingContext("");
		MeshAuthUser requestUser = getUser(rc);
		Page<? extends Node> page = boot.nodeRoot().findAll(requestUser, languageTags, new PagingInfo(1, 10));

		// There are nodes that are only available in english
		assertEquals(data().getNodeCount(), page.getTotalElements());
		assertEquals(10, page.getSize());

		languageTags.add("en");
		page = boot.nodeRoot().findAll(requestUser, languageTags, new PagingInfo(1, 15));
		assertEquals(data().getNodeCount(), page.getTotalElements());
		assertEquals(15, page.getSize());

	}

	@Test
	public void testMeshNodeFields() {
		Node newsNode = content("news overview");
		Language german = german();
		NodeFieldContainer germanFields = newsNode.getOrCreateFieldContainer(german);

		// TODO add some fields

	}

	@Test
	@Override
	public void testFindAllVisible() throws InvalidArgumentException {
		List<String> languageTags = new ArrayList<>();
		languageTags.add("de");
		languageTags.add("en");
		Page<? extends Node> page = boot.nodeRoot().findAll(getRequestUser(), languageTags, new PagingInfo(1, 25));
		assertNotNull(page);

	}

	@Test
	@Override
	public void testRootNode() {
		Project project = project();
		Node root = project.getBaseNode();
		assertNotNull(root);
	}

	@Test
	@Override
	@Ignore("nodes can not be located using the name")
	public void testFindByName() {

	}

	@Test
	@Override
	public void testFindByUUID() {
		Node newsNode = content("news overview");
		boot.nodeRoot().findByUuid(newsNode.getUuid(), rh -> {
			Node node = rh.result();
			assertNotNull(node);
			assertEquals(newsNode.getUuid(), node.getUuid());
		});
	}

	@Test
	@Override
	public void testTransformation() throws InterruptedException, JsonParseException, JsonMappingException, IOException {
		RoutingContext rc = getMockedRoutingContext("lang=en");
		Node newsNode = content("porsche 911");

		CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<NodeResponse> reference = new AtomicReference<>();
		newsNode.transformToRest(rc, rh -> {
			reference.set(rh.result());
			latch.countDown();
		});
		latch.await();
		NodeResponse response = reference.get();

		String json = JsonUtil.toJson(response);
		assertNotNull(json);

		NodeResponse deserialized = JsonUtil.readNode(json, NodeResponse.class, schemaStorage);
		// TODO assert for english fields
		System.out.println(json);

	}

	@Test
	@Override
	public void testCreateDelete() {
		Node folder = folder("2015");
		Node subNode = folder.create(user(), getSchemaContainer(), project());
		assertNotNull(subNode.getUuid());
		subNode.delete();
	}

	@Test
	@Override
	public void testCRUDPermissions() {
		Node node = folder("2015").create(user(), getSchemaContainer(), project());
		assertFalse(user().hasPermission(node, Permission.CREATE_PERM));
		user().addCRUDPermissionOnRole(folder("2015"), Permission.CREATE_PERM, node);
		assertTrue(user().hasPermission(node, Permission.CREATE_PERM));
	}

	@Test
	@Override
	public void testRead() throws IOException {
		Node node = folder("2015");
		assertEquals("folder", node.getSchema().getName());
		assertTrue(node.getSchema().isFolder());
	}

	@Test
	@Override
	public void testCreate() {
		User user = user();
		Node parentNode = folder("2015");
		Node node = parentNode.create(user, data().getSchemaContainer("content"), project());
		long ts = System.currentTimeMillis();
		node.setCreationTimestamp(ts);
		node.setLastEditedTimestamp(ts);
		Long editedTimestamp = node.getLastEditedTimestamp();
		assertNotNull(editedTimestamp);
		assertEquals(ts, editedTimestamp.longValue());
		Long creationTimeStamp = node.getCreationTimestamp();
		assertNotNull(creationTimeStamp);
		assertEquals(ts, creationTimeStamp.longValue());
		assertEquals(user, node.getCreator());
		assertEquals(user, node.getEditor());
		Language english = english();
		Language german = german();

		NodeFieldContainer englishContainer = node.getOrCreateFieldContainer(english);
		englishContainer.createString("content").setString("english content");
		englishContainer.createString("name").setString("english.html");
		assertNotNull(node.getUuid());

		List<? extends FieldContainer> allProperties = node.getFieldContainers();
		assertNotNull(allProperties);
		assertEquals(1, allProperties.size());

		NodeFieldContainer germanContainer = node.getOrCreateFieldContainer(german);
		germanContainer.createString("content").setString("german content");
		assertEquals(2, node.getFieldContainers().size());

		NodeFieldContainer container = node.getFieldContainer(english);
		assertNotNull(container);
		String text = container.getString("content").getString();
		assertNotNull(text);
		assertEquals("english content", text);

	}

	@Test
	@Override
	public void testDelete() {
		Node node = folder("news");
		Map<String, String> uuidToBeDeleted = new HashMap<>();
		for (FieldContainer container : node.getFieldContainers()) {
			uuidToBeDeleted.put("container-" + container.getLanguage().getLanguageTag(), container.getUuid());
		}

		// Add subfolders
		uuidToBeDeleted.put("folder-2015", folder("2015").getUuid());
		uuidToBeDeleted.put("folder-2014", folder("2014").getUuid());

		String uuid = node.getUuid();
		meshRoot().getNodeRoot().findByUuid(uuid, rh -> {
			assertNotNull(rh.result());
		});
		node.delete();
		// TODO check for attached subnodes
		meshRoot().getNodeRoot().findByUuid(uuid, rh -> {
			assertNull(rh.result());
		});

		for (Map.Entry<String, String> entry : uuidToBeDeleted.entrySet()) {
			assertFalse("One vertex was not deleted. Uuid: {" + entry.getValue() + "} - Type: {" + entry.getKey() + "}",
					fg.v().has("uuid", entry.getValue()).hasNext());
		}
	}

	@Test
	@Override
	public void testUpdate() {
		Node node = content();
		User newUser = meshRoot().getUserRoot().create("newUser", group(), user());
		assertEquals(user().getUuid(), node.getCreator().getUuid());
		node.setCreator(newUser);
		assertEquals(newUser.getUuid(), node.getCreator().getUuid());
		// TODO update other fields
	}

	@Test
	@Override
	public void testReadPermission() {
		testPermission(Permission.READ_PERM, content());
	}

	@Test
	@Override
	public void testDeletePermission() {
		testPermission(Permission.DELETE_PERM, content());
	}

	@Test
	@Override
	public void testUpdatePermission() {
		testPermission(Permission.UPDATE_PERM, content());
	}

	@Test
	@Override
	public void testCreatePermission() {
		testPermission(Permission.CREATE_PERM, content());
	}

}
