package com.gentics.cailun.core.data.model;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.service.ContentService;
import com.gentics.cailun.core.repository.TagRepository;
import com.gentics.cailun.path.PagingInfo;
import com.gentics.cailun.test.AbstractDBTest;

public class ContentTest extends AbstractDBTest {

	@Autowired
	ContentService contentService;

	@Autowired
	TagRepository folderRepository;

	@Before
	public void setup() {
		setupData();
	}

	/**
	 * Test linking two contents
	 */
	@Test
	public void testPageLinks() {
		Content content = new Content();
		contentService.setContent(content, data().getEnglish(), "english content");
		contentService.setFilename(content, data().getEnglish(), "english.html");
		contentService.save(content);

		Content content2 = new Content();
		contentService.setContent(content2, data().getEnglish(), "english2 content");
		contentService.setFilename(content2, data().getEnglish(), "english2.html");
		contentService.save(content2);
		contentService.createLink(content, content2);

		// TODO verify that link relation has been created
		// TODO render content and resolve links
	}

	@Test
	public void testFindAll() {

		User user = data().getUserInfo().getUser();
		List<String> languageTags = new ArrayList<>();
		languageTags.add("de");

		Page<Content> page = contentService.findAllVisible(user, "dummy", languageTags, new PagingInfo(0, 10));
		assertEquals(115, page.getTotalElements());
		assertEquals(10, page.getSize());

		languageTags.add("en");
		page = contentService.findAllVisible(user, "dummy", languageTags, new PagingInfo(0, 15));
		assertEquals(117, page.getTotalElements());
		assertEquals(15, page.getSize());

	}

}
