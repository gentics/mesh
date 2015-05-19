package com.gentics.mesh.core.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.apex.RoutingContext;
import io.vertx.ext.apex.Session;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.transaction.NotSupportedException;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.neo4j.conversion.Result;

import com.gentics.mesh.core.data.model.auth.User;
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

		Tag tag = new Tag();
		tagService.setName(tag, german, GERMAN_NAME);
		try (Transaction tx = graphDb.beginTx()) {
			tag = tagService.save(tag);
			tx.success();
		}
		assertNotNull(tag.getId());
		tag = tagService.findOne(tag.getId());
		assertNotNull("The folder could not be found.", tag);
		try (Transaction tx = graphDb.beginTx()) {
			String name = tagService.getName(tag, german);
			assertEquals("The loaded name of the folder did not match the expected one.", GERMAN_NAME, name);
			tx.success();
		}
	}

	@Test
	public void testSimpleTag() {
		Tag tag = new Tag();
		tagService.setProperty(tag, data().getEnglish(), "name", "test");
		tagService.save(tag);
	}

	@Test
	public void testContents() throws NotSupportedException {

		Tag tag = new Tag();

		Language english = languageService.findByLanguageTag("en");

		tagService.setName(tag, english, ENGLISH_NAME);
		try (Transaction tx = graphDb.beginTx()) {
			tag = tagService.save(tag);
			tx.success();
		}
		tag = tagService.findOne(tag.getId());
		assertNotNull(tag);

		final String GERMAN_TEST_FILENAME = "german.html";
		MeshNode content = new MeshNode();

		Language german = languageService.findByLanguageTag("de");

		try (Transaction tx = graphDb.beginTx()) {
			nodeService.setFilename(content, german, GERMAN_TEST_FILENAME);
			nodeService.setName(content, german, "german content name");
			content = nodeService.save(content);
			tag.addContent(content);
			tag = tagService.save(tag);
			tx.success();
		}
		// Reload the tag and check whether the content was set
		try (Transaction tx = graphDb.beginTx()) {

			tag = tagService.reload(tag);
			assertEquals("The tag should have exactly one file.", 1, tag.getContents().size());
			MeshNode contentFromTag = tag.getContents().iterator().next();
			assertNotNull(contentFromTag);
			assertEquals("We did not get the correct content.", content.getId(), contentFromTag.getId());
			String filename = nodeService.getFilename(contentFromTag, german);
			assertEquals("The name of the file from the loaded tag did not match the expected one.", GERMAN_TEST_FILENAME, filename);

			// Remove the file/content and check whether the content was really removed
			assertTrue(tag.removeContent(contentFromTag));
			tag = tagService.save(tag);
			tx.success();
		}
		tag = tagService.reload(tag);
		assertEquals("The tag should not have any file.", 0, tag.getContents().size());

	}

	@Test
	@SuppressWarnings("unchecked")
	public void testQueryBuilding() {
		String query = "MATCH (n:Tag) return n";
		Result<Map<String, Object>> result = neo4jTemplate.query(query, Collections.emptyMap());
		for (Map<String, Object> r : result.slice(1, 3)) {
			Tag tag = (Tag) neo4jTemplate.getDefaultConverter().convert(r.get("n"), Tag.class);
			System.out.println(tag.getUuid());
		}
	}

	@Test
	public void testNodeTagging() {
		Language german = languageService.findByLanguageTag("de");

		// Create root with subfolder
		final String TEST_TAG_NAME = "testTag";

		Tag rootTag = new Tag();
		tagService.setName(rootTag, german, "wurzelordner");

		Tag subFolderTag = new Tag();
		tagService.setName(subFolderTag, german, TEST_TAG_NAME);
		subFolderTag = tagService.save(subFolderTag);

		//		rootTag.addTag(subFolderTag);
		//		tagService.save(rootTag);

		//		Tag reloadedNode = tagService.findOne(rootTag.getId());
		//		assertNotNull("The node shoule be loaded", reloadedNode);
		//		assertTrue("The test node should have a tag with the name {" + TEST_TAG_NAME + "}.", reloadedNode.hasTag(subFolderTag));
		//
		//		Tag extraTag = new Tag();
		//		tagService.setName(extraTag, german, "extra ordner");
		//		assertFalse("The test node should have the random created tag.", reloadedNode.hasTag(extraTag));
		//
		//		try (Transaction tx = graphDb.beginTx()) {
		//			assertTrue("The tag should be removed.", reloadedNode.removeTag(subFolderTag));
		//		}
	}

	@Test
	public void testFindAll() {
		List<String> languageTags = new ArrayList<>();
		languageTags.add("de");
		RoutingContext rc = getMockedRoutingContext();

		Page<Tag> page = tagService.findProjectTags(rc, "dummy", languageTags, new PagingInfo(1, 10));
		assertEquals(11, page.getTotalElements());
		assertEquals(10, page.getSize());

		languageTags.add("en");
		page = tagService.findProjectTags(rc, "dummy", languageTags, new PagingInfo(1, 14));
		assertEquals(20, page.getTotalElements());
		assertEquals(14, page.getSize());
	}

	@Test
	public void testTransformToRest() {
		Tag tag = data().getTag("red");
		assertNotNull("The UUID of the tag must not be null.", tag.getUuid());
		List<String> languageTags = new ArrayList<>();
		languageTags.add("en");
		languageTags.add("de");
		int depth = 3;

		RoutingContext rc = getMockedRoutingContext();
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

	private RoutingContext getMockedRoutingContext() {

		User user = data().getUserInfo().getUser();

		RoutingContext rc = mock(RoutingContext.class);
		Session session = mock(Session.class);
		when(rc.session()).thenReturn(session);
		JsonObject principal = new JsonObject();
		principal.put("uuid", user.getUuid());
		when(session.getPrincipal()).thenReturn(principal);
		// Create login session
		// String loginSessionId = auth.createLoginSession(Long.MAX_VALUE, user);
		// String loginSessionId = null;
		// Session session = mock(Session.class);
		// RoutingContext rc = mock(RoutingContext.class);
		// when(rc.session()).thenReturn(session);
		// when(session.id()).thenReturn(loginSessionId);
		return rc;
	}
}
