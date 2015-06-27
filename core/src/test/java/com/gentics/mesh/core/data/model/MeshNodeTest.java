package com.gentics.mesh.core.data.model;

import static com.gentics.mesh.demo.DemoDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.RoutingContextHelper.getUser;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import io.vertx.ext.web.RoutingContext;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.model.node.MeshNode;
import com.gentics.mesh.core.data.service.transformation.TransformationInfo;
import com.gentics.mesh.core.rest.node.response.NodeResponse;
import com.gentics.mesh.paging.PagingInfo;
import com.gentics.mesh.test.AbstractDBTest;
import com.gentics.mesh.util.InvalidArgumentException;

public class MeshNodeTest extends AbstractDBTest {

	@Before
	public void setup() throws Exception {
		setupData();
	}

	/**
	 * Test linking two contents
	 */
	@Test
	public void testPageLinks() {
		MeshNode folder = data().getFolder("2015");
		MeshNode content = folder.create();
		MeshNode content2 = folder.create();

		MeshNodeFieldContainer englishContainer = content2.getOrCreateFieldContainer(data().getEnglish());
		englishContainer.setProperty("content", "english content");
		englishContainer.setProperty("name", "english.html");

		MeshNodeFieldContainer englishContainer2 = content.getOrCreateFieldContainer(data().getGerman());
		englishContainer2.setProperty("content", "english2 content");
		englishContainer2.setProperty("name", "english2.html");
		nodeService.createLink(content, content2);

		// TODO verify that link relation has been created
		// TODO render content and resolve links
	}

	@Test
	public void testMeshNodeStructure() {
		MeshNode newsNode = data().getContent("news overview");
		assertNotNull(newsNode);
		MeshNode newSubNode;
		newSubNode = newsNode.create();

		assertEquals(1, newsNode.getChildren().size());
		MeshNode firstChild = newsNode.getChildren().iterator().next();
		assertEquals(newSubNode.getUuid(), firstChild.getUuid());
	}

	@Test
	public void testTaggingOfMeshNode() {
		MeshNode newsNode = data().getContent("news overview");
		assertNotNull(newsNode);

		Tag carTag = data().getTag("car");
		assertNotNull(carTag);

		newsNode.addTag(carTag);

		assertEquals(1, newsNode.getTags().size());
		Tag firstTag = newsNode.getTags().iterator().next();
		assertEquals(carTag.getUuid(), firstTag.getUuid());
	}

	@Test
	public void testCreateNode() {
		MeshNode parentNode = data().getFolder("2015");
		MeshNode node = parentNode.create();
		Language english = data().getEnglish();
		Language german = data().getGerman();

		MeshNodeFieldContainer englishContainer = node.getOrCreateFieldContainer(english);
		englishContainer.setProperty("content", "english content");
		englishContainer.setProperty("name", "english.html");
		assertNotNull(node.getUuid());

		List<? extends FieldContainer> allProperties = node.getFieldContainers();
		assertNotNull(allProperties);
		assertEquals(1, allProperties.size());

		MeshNodeFieldContainer germanContainer = node.getOrCreateFieldContainer(german);
		germanContainer.setProperty("content", "german content");
		assertEquals(2, node.getFieldContainers().size());

		MeshNodeFieldContainer container = node.getFieldContainer(english);
		assertNotNull(container);
		String text = container.getProperty("content");
		assertNotNull(text);
		assertEquals("english content", text);
	}

	@Test
	public void testFindAll() throws InvalidArgumentException {

		List<String> languageTags = new ArrayList<>();
		languageTags.add("de");

		RoutingContext rc = getMockedRoutingContext("");
		MeshAuthUser requestUser = getUser(rc);
		Page<? extends MeshNode> page = nodeService.findAll(requestUser, PROJECT_NAME, languageTags, new PagingInfo(1, 10));
		// There are nodes that are only available in english
		assertEquals(data().getNodeCount(), page.getTotalElements());
		assertEquals(10, page.getSize());

		languageTags.add("en");
		page = nodeService.findAll(requestUser, PROJECT_NAME, languageTags, new PagingInfo(1, 15));
		assertEquals(data().getNodeCount(), page.getTotalElements());
		assertEquals(15, page.getSize());

	}

	@Test
	public void testTransformToRest() {
		RoutingContext rc = getMockedRoutingContext("");
		MeshAuthUser requestUser = getUser(rc);
		List<String> languageTags = new ArrayList<>();
		languageTags.add("en");
		MeshNode newsNode = data().getContent("news overview");
		TransformationInfo info = new TransformationInfo(requestUser, languageTags, rc);
		NodeResponse response = newsNode.transformToRest(info);
		assertNotNull(response);

	}

}
