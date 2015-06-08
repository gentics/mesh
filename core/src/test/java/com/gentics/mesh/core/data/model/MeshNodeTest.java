package com.gentics.mesh.core.data.model;

import static com.gentics.mesh.util.TinkerpopUtils.count;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import io.vertx.ext.apex.RoutingContext;

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

			nodeService.setContent(content, data().getEnglish(), "english content");
			nodeService.setName(content, data().getEnglish(), "english.html");
			nodeService.save(content);

			nodeService.setContent(content2, data().getEnglish(), "english2 content");
			nodeService.setName(content2, data().getEnglish(), "english2.html");
			nodeService.save(content2);
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
//		try (Transaction tx = graphDb.beginTx()) {
			newSubNode = nodeService.create();
			newSubNode.setParentNode(newsNode);
			nodeService.save(newSubNode);
//			tx.success();
//		}

//		try (Transaction tx = graphDb.beginTx()) {
			newsNode = nodeService.reload(newsNode);
			assertEquals(1, count(newsNode.getChildren()));
			MeshNode firstChild = newsNode.getChildren().iterator().next();
//			neo4jTemplate.fetch(firstChild);
			assertEquals(newSubNode.getUuid(), firstChild.getUuid());
//			tx.success();
//		}
	}

	@Test
	public void testTaggingOfMeshNode() {
		MeshNode newsNode = data().getContent("news overview");
		assertNotNull(newsNode);

		Tag carTag = data().getTag("car");
		assertNotNull(carTag);

//		try (Transaction tx = graphDb.beginTx()) {

			newsNode.addTag(carTag);
			nodeService.save(newsNode);

			// Reload node 
			newsNode = nodeService.reload(newsNode);

			assertEquals(1, count(newsNode.getTags()));
			Tag firstTag = newsNode.getTags().iterator().next();
//			neo4jTemplate.fetch(firstTag);
			assertEquals(carTag.getUuid(), firstTag.getUuid());
//			tx.success();
//		}
	}

	@Test
	public void testCreateNode() {
		MeshNode node = nodeService.create();
//		try (Transaction tx = graphDb.beginTx()) {
			nodeService.setContent(node, data().getEnglish(), "english content");
			nodeService.setName(node, data().getEnglish(), "english.html");
			node = nodeService.save(node);
//			tx.success();
//		}
		node = nodeService.reload(node);
		assertNotNull(node.getUuid());
//		try (Transaction tx = graphDb.beginTx()) {
			String text = nodeService.getContent(node, data().getEnglish());
			assertNotNull(text);
//			tx.success();
//		}
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
