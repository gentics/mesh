package com.gentics.mesh.core;

import static com.gentics.mesh.demo.DemoDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.RoutingContextHelper.getUser;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import io.vertx.ext.web.RoutingContext;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.data.FieldContainer;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.service.BasicObjectTestcases;
import com.gentics.mesh.core.data.service.transformation.TransformationInfo;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.paging.PagingInfo;
import com.gentics.mesh.test.AbstractDBTest;
import com.gentics.mesh.util.InvalidArgumentException;

public class NodeTest extends AbstractDBTest implements BasicObjectTestcases {

	@Before
	public void setup() throws Exception {
		setupData();
	}

	/**
	 * Test linking two contents
	 */
	@Test
	public void testPageLinks() {
		Node folder = data().getFolder("2015");
		Node content = folder.create();
		Node content2 = folder.create();

		NodeFieldContainer englishContainer = content2.getOrCreateFieldContainer(data().getEnglish());
		englishContainer.createString("content").setString("english content");
		englishContainer.createString("name").setString("english.html");

		NodeFieldContainer englishContainer2 = content.getOrCreateFieldContainer(data().getGerman());
		englishContainer2.createString("content").setString("english2 content");
		englishContainer2.createString("name").setString("english2.html");
		nodeService.createLink(content, content2);

		// TODO verify that link relation has been created
		// TODO render content and resolve links
	}

	@Test
	public void testMeshNodeStructure() {
		Node newsNode = data().getContent("news overview");
		assertNotNull(newsNode);
		Node newSubNode;
		newSubNode = newsNode.create();

		assertEquals(1, newsNode.getChildren().size());
		Node firstChild = newsNode.getChildren().iterator().next();
		assertEquals(newSubNode.getUuid(), firstChild.getUuid());
	}

	@Test
	public void testTaggingOfMeshNode() {
		Node newsNode = data().getContent("news overview");
		assertNotNull(newsNode);

		Tag carTag = data().getTag("car");
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
		Page<? extends Node> page = nodeService.findAll(requestUser, PROJECT_NAME, languageTags, new PagingInfo(1, 10));
		// There are nodes that are only available in english
		assertEquals(data().getNodeCount(), page.getTotalElements());
		assertEquals(10, page.getSize());

		languageTags.add("en");
		page = nodeService.findAll(requestUser, PROJECT_NAME, languageTags, new PagingInfo(1, 15));
		assertEquals(data().getNodeCount(), page.getTotalElements());
		assertEquals(15, page.getSize());

	}

	@Test
	public void testMeshNodeFields() {
		Node newsNode = data().getContent("news overview");
		Language german = data().getGerman();
		NodeFieldContainer germanFields = newsNode.getOrCreateFieldContainer(german);

		// TODO add some fields

	}

	@Test
	@Override
	public void testFindAllVisible() throws InvalidArgumentException {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testRootNode() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	@Ignore("nodes can not be located using the name")
	public void testFindByName() {

	}

	@Test
	@Override
	public void testFindByUUID() {
		Node newsNode = data().getContent("news overview");
		Node node = nodeService.findByUUID(newsNode.getUuid());
		assertNotNull(node);
		assertEquals(newsNode.getUuid(), node.getUuid());
	}

	@Test
	@Override
	public void testTransformation() {
		RoutingContext rc = getMockedRoutingContext("");
		MeshAuthUser requestUser = getUser(rc);
		List<String> languageTags = new ArrayList<>();
		languageTags.add("en");
		Node newsNode = data().getContent("porsche 911");
		TransformationInfo info = new TransformationInfo(requestUser, languageTags, rc);
		NodeResponse response = newsNode.transformToRest(info);
		assertNotNull(response);
		System.out.println(JsonUtil.toJson(response));

	}

	@Test
	@Override
	public void testCreateDelete() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testCRUDPermissions() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testPermissionsOnObject() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testRead() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testCreate() {
		User user = data().getUserInfo().getUser();
		Node parentNode = data().getFolder("2015");
		Node node = parentNode.create();
		node.setCreator(user);
		long ts = System.currentTimeMillis();
		node.setCreationTimestamp(ts);
		node.setEditor(user);
		node.setLastEditedTimestamp(ts);
		Long editedTimestamp = node.getLastEditedTimestamp();
		assertNotNull(editedTimestamp);
		assertEquals(ts, editedTimestamp.longValue());
		Long creationTimeStamp = node.getCreationTimestamp();
		assertNotNull(creationTimeStamp);
		assertEquals(ts, creationTimeStamp.longValue());
		assertEquals(user, node.getCreator());
		assertEquals(user, node.getEditor());
		Language english = data().getEnglish();
		Language german = data().getGerman();

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
		// TODO Auto-generated method stub

	}

	@Override
	public void testUpdate() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testReadPermission() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testDeletePermission() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testUpdatePermission() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testCreatePermission() {
		// TODO Auto-generated method stub

	}

}
