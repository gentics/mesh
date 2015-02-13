package com.gentics.cailun.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.repository.GlobalLocalizedContentRepository;
import com.gentics.cailun.core.repository.GlobalLocalizedTagRepository;
import com.gentics.cailun.core.rest.model.Content;
import com.gentics.cailun.core.rest.model.LocalizedContent;
import com.gentics.cailun.core.rest.model.LocalizedTag;
import com.gentics.cailun.core.rest.model.Tag;
import com.gentics.cailun.test.Neo4jSpringTestConfiguration;
import com.gentics.cailun.util.Neo4jGenericContentUtils;

@ContextConfiguration(classes = { Neo4jSpringTestConfiguration.class })
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class Neo4jGenericContentUtilsTest {

	@Autowired
	private Neo4jGenericContentUtils neo4jPageUtils;

	@Autowired
	private GlobalLocalizedContentRepository contentRepository;

	@Autowired
	private GlobalLocalizedTagRepository tagRepository;

	@Autowired
	private GraphDatabaseService graphDb;

	/**
	 * Test the getPath method with a simple tag page hierarchy (roottag->subtag->test page).
	 */
	@Transactional
	@Test
	public void testSimplePagePathTraversal() {

		Tag rootTag = new Tag();
		rootTag.addLocalisation(new LocalizedTag("rootTag"));

		Tag subTag = new Tag();
		subTag.addLocalisation(new LocalizedTag("subTag"));

		rootTag.addChildTag(subTag);

		tagRepository.save(subTag);
		tagRepository.save(rootTag);

		LocalizedContent germanContent = new LocalizedContent("test content");
		germanContent.setFilename("test.html");

		Content content = new Content();
		content.addLocalisation(germanContent);

		contentRepository.save(content);

//		String path = neo4jPageUtils.getPath(rootTag, content);
//		assertEquals("The path did not match the expected one.", "/root/subtag/test.html", path);
	}

	/**
	 * Test the getPath method with a more realistic tag page hierarchy.
	 */
	@Transactional
	@Test
	public void testComplexPagePathTraversal() {

		Tag rootTag = new Tag();
		rootTag.addLocalisation(new LocalizedTag("rootTag"));

		Tag subTag = new Tag();
		subTag.addLocalisation(new LocalizedTag("subTag"));

		rootTag.addChildTag(subTag);

		Tag subTag2 = new Tag();
		subTag2.addLocalisation(new LocalizedTag("subTag 2"));

		tagRepository.save(subTag);
		tagRepository.save(subTag2);
		tagRepository.save(rootTag);

		LocalizedContent content = new LocalizedContent("test content");
		content.setFilename("test.html");
//		content.tag(subTag);

		contentRepository.save(content);
		tagRepository.save(rootTag);

		LocalizedContent page2 = new LocalizedContent("test content 2");
		page2.setFilename("test2.html");
//		page2.tag(subTag);
//		contentRepository.save(page2);

//		String path = neo4jPageUtils.getPath(rootTag, content);
//		assertEquals("The resolved path did not match the expected one.", "/root/subtag/test.html", path);
	}
}
