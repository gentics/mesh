package com.gentics.mesh.core.node;

import static com.gentics.mesh.util.MeshAssert.assertDeleted;
import static com.gentics.mesh.util.MeshAssert.failingLatch;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.query.impl.PagingParameter;
import com.gentics.mesh.test.AbstractBasicObjectTest;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.MeshAssert;

import io.vertx.ext.web.RoutingContext;
import rx.Observable;

public class NodeTest extends AbstractBasicObjectTest {

	@Autowired
	private ServerSchemaStorage schemaStorage;

	@Test
	@Override
	public void testTransformToReference() throws Exception {
		Node node = content();
		InternalActionContext ac = getMockedInternalActionContext("");
		NodeReference reference = node.transformToReference(ac).toBlocking().first();
		assertNotNull(reference);
		assertEquals(node.getUuid(), reference.getUuid());
	}

	//	/**
	//	 * Test linking two contents
	//	 */
	//	@Test
	//	public void testPageLinks() {
	//		Node folder = folder("2015");
	//		Node node = folder.create(user(), getSchemaContainer(), project());
	//		Node node2 = folder.create(user(), getSchemaContainer(), project());
	//
	//		NodeGraphFieldContainer englishContainer = node2.getOrCreateGraphFieldContainer(english());
	//		englishContainer.createString("content").setString("english content");
	//		englishContainer.createString("name").setString("english.html");
	//
	//		NodeGraphFieldContainer englishContainer2 = node.getOrCreateGraphFieldContainer(german());
	//		englishContainer2.createString("content").setString("english2 content");
	//		englishContainer2.createString("name").setString("english2.html");
	//		node.createLink(node2);
	//
	//		// TODO verify that link relation has been created
	//		// TODO render content and resolve links
	//	}

	@Test
	public void testGetPath() throws Exception {
		Node newsNode = content("news overview");
		CountDownLatch latch = new CountDownLatch(2);
		Observable<String> path = newsNode.getPath(english().getLanguageTag());
		path.subscribe(s -> {
			assertEquals("/News/News+Overview.en.html", s);
			latch.countDown();
		});

		Observable<String> pathSegementFieldValue = newsNode.getPathSegment(english().getLanguageTag());
		pathSegementFieldValue.subscribe(s -> {
			assertEquals("News Overview.en.html", s);
			latch.countDown();
		});
		failingLatch(latch);
	}

	@Test
	public void testMeshNodeStructure() {
		Node newsNode = content("news overview");
		assertNotNull(newsNode);
		Node newSubNode;
		newSubNode = newsNode.create(user(), getSchemaContainer().getLatestVersion(), project());

		assertEquals(1, newsNode.getChildren().size());
		Node firstChild = newsNode.getChildren().iterator().next();
		assertEquals(newSubNode.getUuid(), firstChild.getUuid());
	}

	@Test
	public void testGetSegmentPath() {
		Node newsNode = content("news overview");
		RoutingContext rc = getMockedRoutingContext("");
		InternalActionContext ac = InternalActionContext.create(rc);
		assertNotNull(newsNode.getPathSegment(ac));
	}

	@Test
	public void testGetFullSegmentPath() {
		Node newsNode = content("news overview");
		RoutingContext rc = getMockedRoutingContext("");
		InternalActionContext ac = InternalActionContext.create(rc);
		System.out.println(newsNode.getPath(ac));
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
		InternalActionContext ac = InternalActionContext.create(rc);
		MeshAuthUser requestUser = ac.getUser();
		PageImpl<? extends Node> page = boot.nodeRoot().findAll(requestUser, languageTags, new PagingParameter(1, 10));

		// There are nodes that are only available in english
		assertEquals(getNodeCount(), page.getTotalElements());
		assertEquals(10, page.getSize());

		languageTags.add("en");
		page = boot.nodeRoot().findAll(requestUser, languageTags, new PagingParameter(1, 15));
		assertEquals(getNodeCount(), page.getTotalElements());
		assertEquals(15, page.getSize());
	}

	@Test
	public void testMeshNodeFields() throws IOException {
		Node newsNode = content("news overview");
		Language german = german();
		RoutingContext rc = getMockedRoutingContext("?lang=de,en");
		InternalActionContext ac = InternalActionContext.create(rc);
		NodeGraphFieldContainer germanFields = newsNode.createGraphFieldContainer(german, newsNode.getSchemaContainer().getLatestVersion());
		assertEquals(germanFields.getString(newsNode.getSchemaContainer().getLatestVersion().getSchema().getDisplayField()).getString(),
				newsNode.getDisplayName(ac));
		// TODO add some fields
	}

	@Test
	@Override
	public void testFindAllVisible() throws InvalidArgumentException {
		List<String> languageTags = new ArrayList<>();
		languageTags.add("de");
		languageTags.add("en");
		PageImpl<? extends Node> page = boot.nodeRoot().findAll(getRequestUser(), languageTags, new PagingParameter(1, 25));
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
	public void testFindByUUID() throws Exception {
		Node newsNode = content("news overview");
		Node node = boot.nodeRoot().findByUuid(newsNode.getUuid()).toBlocking().first();
		assertNotNull(node);
		assertEquals(newsNode.getUuid(), node.getUuid());
	}

	@Test
	@Override
	public void testTransformation() throws Exception {
		RoutingContext rc = getMockedRoutingContext("lang=en");
		InternalActionContext ac = InternalActionContext.create(rc);
		Node newsNode = content("concorde");

		NodeResponse response = newsNode.transformToRest(ac).toBlocking().first();
		String json = JsonUtil.toJson(response);
		assertNotNull(json);

		NodeResponse deserialized = JsonUtil.readNode(json, NodeResponse.class, schemaStorage);
		assertNotNull(deserialized);
		// TODO assert for english fields
	}

	@Test
	@Override
	public void testCreateDelete() {
		Node folder = folder("2015");
		Node subNode = folder.create(user(), getSchemaContainer().getLatestVersion(), project());
		assertNotNull(subNode.getUuid());
		subNode.delete();
	}

	@Test
	@Override
	public void testCRUDPermissions() {
		Node node = folder("2015").create(user(), getSchemaContainer().getLatestVersion(), project());
		InternalActionContext ac = getMockedInternalActionContext("");
		assertFalse(user().hasPermissionAsync(ac, node, GraphPermission.CREATE_PERM).toBlocking().first());
		user().addCRUDPermissionOnRole(folder("2015"), GraphPermission.CREATE_PERM, node);
		ac.data().clear();
		assertTrue(user().hasPermissionAsync(ac, node, GraphPermission.CREATE_PERM).toBlocking().first());
	}

	@Test
	@Override
	public void testRead() throws IOException {
		Node node = folder("2015");
		assertEquals("folder", node.getSchemaContainer().getLatestVersion().getSchema().getName());
		assertTrue(node.getSchemaContainer().getLatestVersion().getSchema().isContainer());
	}

	@Test
	@Override
	public void testCreate() {
		User user = user();
		Node parentNode = folder("2015");
		SchemaContainerVersion schemaVersion = schemaContainer("content").getLatestVersion();
		Node node = parentNode.create(user, schemaVersion, project());
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

		NodeGraphFieldContainer englishContainer = node.createGraphFieldContainer(english, schemaVersion);
		englishContainer.createString("content").setString("english content");
		englishContainer.createString("name").setString("english.html");
		assertNotNull(node.getUuid());

		List<? extends GraphFieldContainer> allProperties = node.getGraphFieldContainers();
		assertNotNull(allProperties);
		assertEquals(1, allProperties.size());

		NodeGraphFieldContainer germanContainer = node.createGraphFieldContainer(german, schemaVersion);
		germanContainer.createString("content").setString("german content");
		assertEquals(2, node.getGraphFieldContainers().size());

		NodeGraphFieldContainer container = node.getGraphFieldContainer(english);
		assertNotNull(container);
		String text = container.getString("content").getString();
		assertNotNull(text);
		assertEquals("english content", text);

	}

	@Test
	@Override
	public void testDelete() throws Exception {
		Map<String, String> uuidToBeDeleted = new HashMap<>();
		String uuid;
		Node node = folder("news");
		for (GraphFieldContainer container : node.getGraphFieldContainers()) {
			uuidToBeDeleted.put("container-" + container.getLanguage().getLanguageTag(), container.getUuid());
		}

		// Add subfolders
		uuidToBeDeleted.put("folder-2015", folder("2015").getUuid());
		uuidToBeDeleted.put("folder-2014", folder("2014").getUuid());

		uuid = node.getUuid();
		MeshAssert.assertElement(meshRoot().getNodeRoot(), uuid, true);
		try (Trx tx = db.trx()) {
			node.delete();
			tx.success();
		}

		// TODO check for attached subnodes
		MeshAssert.assertElement(meshRoot().getNodeRoot(), uuid, false);

		assertDeleted(uuidToBeDeleted);
	}

	@Test
	@Override
	public void testUpdate() {
		Node node = content();
		try (Trx tx = db.trx()) {
			User newUser = meshRoot().getUserRoot().create("newUser", user());
			newUser.addGroup(group());
			assertEquals(user().getUuid(), node.getCreator().getUuid());
			System.out.println(newUser.getUuid());
			node.setCreator(newUser);
			System.out.println(node.getCreator().getUuid());

			assertEquals(newUser.getUuid(), node.getCreator().getUuid());
			// TODO update other fields
		}
	}

	@Test
	@Override
	public void testReadPermission() {
		testPermission(GraphPermission.READ_PERM, content());
	}

	@Test
	@Override
	public void testDeletePermission() {
		testPermission(GraphPermission.DELETE_PERM, content());
	}

	@Test
	@Override
	public void testUpdatePermission() {
		testPermission(GraphPermission.UPDATE_PERM, content());
	}

	@Test
	@Override
	public void testCreatePermission() {
		testPermission(GraphPermission.CREATE_PERM, content());
	}

}
