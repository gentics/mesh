package com.gentics.cailun.core.rest.model;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.repository.GlobalContentRepository;
import com.gentics.cailun.core.repository.GlobalFolderTagRepository;
import com.gentics.cailun.test.AbstractDBTest;

public class ContentTest extends AbstractDBTest {

	@Autowired
	GlobalContentRepository contentRepository;

//	@Autowired
//	GlobalTagRepository<Tag<LocalizedTag, Content<LocalizedContent>, File>> tagRepository;

	@Autowired
	GlobalFolderTagRepository folderRepository;

	@Test
	public void testPageLinks() {
		// Content content = new Content();
		// content.addLocalisation(23);
		// Content content2 = new Content();
		// contentRepository.save(content);
		// content.linkTo(content2);
		// contentRepository.save(content2);
	}

	/**
	 * Test the getPath method with a simple tag page hierarchy (roottag->subtag->test page).
	 */
	@Transactional
	@Test
	public void testSimplePagePathTraversal() {

//		FolderTag rootTag = new FolderTag(german, "rootTag");

//		FolderTag subTag = new FolderTag(german, "subTag");

//		rootTag.addChildTag(subTag);
//
//		folderRepository.save(subTag);
//		folderRepository.save(rootTag);
//
//		Content content = new Content(german, "test", "german.html");
//		content.addI18Content(english, "englishContent");
//		content.setFilename(english, "english.html");
//
//		contentRepository.save(content);
//
		// String path = neo4jPageUtils.getPath(rootTag, content);
		// assertEquals("The path did not match the expected one.", "/root/subtag/test.html", path);
	}

	/**
	 * Test the getPath method with a more realistic tag page hierarchy.
	 */
	@Transactional
	@Test
	public void testComplexPagePathTraversal() {

//		FolderTag rootTag = new FolderTag(german, "rootTag");
//		FolderTag subTag = new FolderTag(german, "subTag");
//
//		rootTag.addChildTag(subTag);
//		FolderTag subTag2 = new FolderTag(german, "subTag 2");
//
//		tagRepository.save(subTag);
//		tagRepository.save(subTag2);
//		tagRepository.save(rootTag);
//
//		Content content = new Content(german, "test content", "test.html");
//		// content.tag(subTag);
//
//		contentRepository.save(content);
//		tagRepository.save(rootTag);
//
//		LocalizedContent page2 = new LocalizedContent(english, "test content 2", "english2.html");
//		page2.setFilename("test2.html");
//		// page2.tag(subTag);
		// contentRepository.save(page2);

		// String path = neo4jPageUtils.getPath(rootTag, content);
		// assertEquals("The resolved path did not match the expected one.", "/root/subtag/test.html", path);
	}
}
