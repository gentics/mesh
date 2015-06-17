package com.gentics.mesh.core.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import io.vertx.ext.web.RoutingContext;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.model.tinkerpop.MeshNode;
import com.gentics.mesh.core.data.model.tinkerpop.Tag;
import com.gentics.mesh.demo.DemoDataProvider;
import com.gentics.mesh.paging.PagingInfo;
import com.gentics.mesh.test.AbstractDBTest;

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
		MeshNode content = nodeService.create();
		MeshNode content2 = nodeService.create();
		//		try (Transaction tx = graphDb.beginTx()) {

		content.setContent(data().getEnglish(), "english content");
		content.setName(data().getEnglish(), "english.html");

		content2.setContent(data().getEnglish(), "english2 content");
		content2.setName(data().getEnglish(), "english2.html");
		//			tx.success();
		//		}
		nodeService.createLink(content, content2);

		// TODO verify that link relation has been created
		// TODO render content and resolve links
	}

	@Test
	public void testMeshNodeStructure() {
		MeshNode newsNode = data().getContent("news overview");
		assertNotNull(newsNode);
		MeshNode newSubNode;
		newSubNode = nodeService.create();
		newSubNode.setParentNode(newsNode);

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
		MeshNode node = nodeService.create();
		node.setContent(data().getEnglish(), "english content");
		node.setName(data().getEnglish(), "english.html");
		assertNotNull(node.getUuid());
		String text = node.getContent(data().getEnglish());
		assertNotNull(text);
	}

	@Test
	public void testFindAll() {

		List<String> languageTags = new ArrayList<>();
		languageTags.add("de");

		RoutingContext rc = getMockedRoutingContext("");

		Page<MeshNode> page = nodeService.findAll(rc, DemoDataProvider.PROJECT_NAME, languageTags, new PagingInfo(1, 10));
		// There are nodes that are only available in english
		assertEquals(data().getNodeCount() - 5, page.getTotalElements());
		assertEquals(10, page.getSize());

		languageTags.add("en");
		page = nodeService.findAll(rc, "dummy", languageTags, new PagingInfo(1, 15));
		assertEquals(data().getNodeCount(), page.getTotalElements());
		assertEquals(15, page.getSize());

	}

}
