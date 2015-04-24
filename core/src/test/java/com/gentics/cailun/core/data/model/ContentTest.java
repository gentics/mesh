package com.gentics.cailun.core.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.service.ContentService;
import com.gentics.cailun.core.repository.TagRepository;
import com.gentics.cailun.demo.DemoDataProvider;
import com.gentics.cailun.paging.PagingInfo;
import com.gentics.cailun.test.AbstractDBTest;

public class ContentTest extends AbstractDBTest {

	@Autowired
	ContentService contentService;

	@Autowired
	TagRepository folderRepository;

	@Before
	public void setup() throws Exception {
		setupData();
	}

	/**
	 * Test linking two contents
	 */
	@Test
	public void testPageLinks() {
		Content content = new Content();
		Content content2 = new Content();
		try (Transaction tx = graphDb.beginTx()) {

			contentService.setContent(content, data().getEnglish(), "english content");
			contentService.setFilename(content, data().getEnglish(), "english.html");
			contentService.save(content);

			contentService.setContent(content2, data().getEnglish(), "english2 content");
			contentService.setFilename(content2, data().getEnglish(), "english2.html");
			contentService.save(content2);
			tx.success();
		}
		contentService.createLink(content, content2);

		// TODO verify that link relation has been created
		// TODO render content and resolve links
	}

	@Test
	public void testCreateContent() {
		Content content = new Content();
		try (Transaction tx = graphDb.beginTx()) {
			contentService.setContent(content, data().getEnglish(), "english content");
			contentService.setFilename(content, data().getEnglish(), "english.html");
			content = contentService.save(content);
			tx.success();
		}
		content = contentService.reload(content);
		assertNotNull(content.getUuid());
		try (Transaction tx = graphDb.beginTx()) {
			String text = contentService.getContent(content, data().getEnglish());
			assertNotNull(text);
			tx.success();
		}
	}

	@Test
	public void testFindAll() {

		User user = data().getUserInfo().getUser();
		List<String> languageTags = new ArrayList<>();
		languageTags.add("de");

		Page<Content> page = contentService.findAllVisible(user, DemoDataProvider.PROJECT_NAME, languageTags, new PagingInfo(1, 10));
		// There are contents that are only available in english
		assertEquals(data().getTotalContents() - 2, page.getTotalElements());
		assertEquals(10, page.getSize());

		languageTags.add("en");
		page = contentService.findAllVisible(user, "dummy", languageTags, new PagingInfo(1, 15));
		assertEquals(data().getTotalContents(), page.getTotalElements());
		assertEquals(15, page.getSize());

	}

}
