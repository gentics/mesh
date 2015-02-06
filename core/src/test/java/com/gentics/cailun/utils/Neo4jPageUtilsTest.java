package com.gentics.cailun.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.repository.PageRepository;
import com.gentics.cailun.core.repository.TagRepository;
import com.gentics.cailun.core.rest.model.Page;
import com.gentics.cailun.core.rest.model.Tag;
import com.gentics.cailun.test.Neo4jSpringTestConfiguration;
import com.gentics.cailun.util.Neo4jPageUtils;

@ContextConfiguration(classes = { Neo4jSpringTestConfiguration.class })
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class Neo4jPageUtilsTest {

	@Autowired
	private Neo4jPageUtils neo4jPageUtils;

	@Autowired
	private PageRepository pageRepository;

	@Autowired
	private TagRepository tagRepository;

	@Autowired
	private GraphDatabaseService graphDb;

	/**
	 * Test the getPath method with a simple tag page hierarchy (roottag->subtag->test page).
	 */
	@Transactional
	@Test
	public void testSimplePagePathTraversal() {
		Page page = new Page("test page");
		page.setFilename("test.html");

		Tag rootTag = new Tag("root");
		Tag subTag = rootTag.tag("subtag");
		page.tag(subTag);

		pageRepository.save(page);
		tagRepository.save(rootTag);
		tagRepository.save(subTag);

		String path = neo4jPageUtils.getPath(rootTag, page);
		assertEquals("", "/root/subtag/test.html", path);
	}

	/**
	 * Test the getPath method with a more realistic tag page hierarchy.
	 */
	@Transactional
	@Test
	public void testComplexPagePathTraversal() {
		Page page = new Page("test page");
		page.setFilename("test.html");

		Tag rootTag = new Tag("root");
		Tag subTag = rootTag.tag("subtag");
		page.tag(subTag);

		pageRepository.save(page);
		tagRepository.save(rootTag);
		tagRepository.save(subTag);

		Page page2 = new Page("test page 2");
		page2.setFilename("test2.html");
		page2.tag(subTag);
		pageRepository.save(page2);

		Tag subTag2 = rootTag.tag("subtag2");
		tagRepository.save(subTag2);
		tagRepository.save(rootTag);

		String path = neo4jPageUtils.getPath(rootTag, page);
		assertEquals("", "/root/subtag/test.html", path);
	}
}
