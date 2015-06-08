package com.gentics.mesh.core.data.model;

import static com.gentics.mesh.util.TinkerpopUtils.count;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.apex.RoutingContext;

import java.util.ArrayList;
import java.util.List;

import javax.transaction.NotSupportedException;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.model.tinkerpop.Language;
import com.gentics.mesh.core.data.model.tinkerpop.MeshNode;
import com.gentics.mesh.core.data.model.tinkerpop.Tag;
import com.gentics.mesh.core.data.service.MeshNodeService;
import com.gentics.mesh.core.data.service.TagService;
import com.gentics.mesh.core.rest.tag.response.TagResponse;
import com.gentics.mesh.paging.PagingInfo;
import com.gentics.mesh.test.AbstractDBTest;
import com.gentics.mesh.util.JsonUtils;

public class TagTest extends AbstractDBTest {

	private static Logger log = LoggerFactory.getLogger(TagTest.class);

	public static final String GERMAN_NAME = "test german name";

	public static final String ENGLISH_NAME = "test english name";

	@Autowired
	private TagService tagService;

	@Autowired
	private MeshNodeService nodeService;

	@Before
	public void setup() throws Exception {
		setupData();
	}

	@Test
	public void testLocalizedFolder() {
		Language german = languageService.findByLanguageTag("de");

		Tag tag = tagService.create();
		tagService.setDisplayName(tag, german, GERMAN_NAME);
//		try (Transaction tx = graphDb.beginTx()) {
			tag = tagService.save(tag);
//			tx.success();
//		}
		assertNotNull(tag.getId());
		tag = tagService.findOne(tag.getId());
		assertNotNull("The folder could not be found.", tag);
//		try (Transaction tx = graphDb.beginTx()) {
			String name = tagService.getDisplayName(tag, german);
			assertEquals("The loaded name of the folder did not match the expected one.", GERMAN_NAME, name);
//			tx.success();
//		}
	}

	@Test
	public void testSimpleTag() {
		Tag tag = tagService.create();
		tagService.setProperty(tag, data().getEnglish(), "name", "test");
		tagService.save(tag);
	}

	@Test
	public void testNodes() throws NotSupportedException {

		Tag tag = tagService.create();

		Language english = languageService.findByLanguageTag("en");

		tagService.setDisplayName(tag, english, ENGLISH_NAME);
//		try (Transaction tx = graphDb.beginTx()) {
			tag = tagService.save(tag);
//			tx.success();
//		}
		tag = tagService.findOne(tag.getId());
		assertNotNull(tag);

		final String GERMAN_TEST_FILENAME = "german.html";
		MeshNode node = nodeService.create();

		Language german = languageService.findByLanguageTag("de");

//		try (Transaction tx = graphDb.beginTx()) {
			nodeService.setDisplayName(node, german, GERMAN_TEST_FILENAME);
			nodeService.setName(node, german, "german node name");
			tag = tagService.save(tag);

			// Assign the tag to the node
			node.addTag(tag);
			node = nodeService.save(node);
//			tx.success();
//		}

		// Reload the tag and check whether the content was set
//		try (Transaction tx = graphDb.beginTx()) {

			tag = tagService.reload(tag);

			assertEquals("The tag should have exactly one node.", 1, count(tag.getNodes()));
			MeshNode contentFromTag = tag.getNodes().iterator().next();
			assertNotNull(contentFromTag);
			assertEquals("We did not get the correct content.", node.getId(), contentFromTag.getId());
			String filename = nodeService.getDisplayName(contentFromTag, german);
			assertEquals("The name of the file from the loaded tag did not match the expected one.", GERMAN_TEST_FILENAME, filename);

			// Remove the file/content and check whether the content was really removed
			tag.removeNode(contentFromTag);
			//TODO verify for removed node
			tag = tagService.save(tag);
//			tx.success();
//		}
		tag = tagService.reload(tag);
		assertEquals("The tag should not have any file.", 0, count(tag.getNodes()));

	}

	//	@Test
	//	@SuppressWarnings("unchecked")
	//	public void testQueryBuilding() {
	//		String query = "MATCH (n:Tag) return n";
	//		Result<Map<String, Object>> result = neo4jTemplate.query(query, Collections.emptyMap());
	//		for (Map<String, Object> r : result.slice(1, 3)) {
	//			Tag tag = (Tag) neo4jTemplate.getDefaultConverter().convert(r.get("n"), Tag.class);
	//			System.out.println(tag.getUuid());
	//		}
	//	}

	@Test
	public void testNodeTagging() {
		Language german = languageService.findByLanguageTag("de");
		final String TEST_TAG_NAME = "testTag";

		MeshNode node = data().getFolder("news");

		Tag tag = tagService.create();
		tagService.setDisplayName(tag, german, TEST_TAG_NAME);
		tag = tagService.save(tag);
		node.addTag(tag);
		nodeService.save(node);

		MeshNode reloadedNode = nodeService.reload(node);
		boolean found = false;
		for (Tag currentTag : reloadedNode.getTags()) {
//			neo4jTemplate.fetch(currentTag);
			if (currentTag.getUuid().equals(tag.getUuid())) {
				found = true;
			}
		}

		assertTrue("The tag {" + tag.getUuid() + "} was not found within the node tags.", found);
	}

	@Test
	public void testFindAll() {
		List<String> languageTags = new ArrayList<>();
		languageTags.add("de");
		RoutingContext rc = getMockedRoutingContext("");

		Page<Tag> tagPage = tagService.findProjectTags(rc, "dummy", languageTags, new PagingInfo(1, 10));
		assertEquals(8, tagPage.getTotalElements());
		assertEquals(10, tagPage.getSize());

		languageTags.add("en");
		tagPage = tagService.findProjectTags(rc, "dummy", languageTags, new PagingInfo(1, 14));
		assertEquals(data().getTags().size(), tagPage.getTotalElements());
		assertEquals(14, tagPage.getSize());
	}

	@Test
	public void testTransformToRest() {
		Tag tag = data().getTag("red");
		assertNotNull("The UUID of the tag must not be null.", tag.getUuid());
		List<String> languageTags = new ArrayList<>();
		languageTags.add("en");
		languageTags.add("de");
		int depth = 3;

		RoutingContext rc = getMockedRoutingContext("lang=de,en");
		for (int i = 0; i < 100; i++) {
			long start = System.currentTimeMillis();
			TagResponse response = tagService.transformToRest(rc, tag);
			assertNotNull(response);
			long dur = System.currentTimeMillis() - start;
			log.info("Transformation with depth {" + depth + "} took {" + dur + "} [ms]");
			System.out.println(JsonUtils.toJson(response));
		}
		// assertEquals(2, response.getChildTags().size());
		// assertEquals(4, response.getPerms().length);
	}

}
