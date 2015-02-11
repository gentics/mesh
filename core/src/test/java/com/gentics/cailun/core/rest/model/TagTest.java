package com.gentics.cailun.core.rest.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.repository.CaiLunNodeRepository;
import com.gentics.cailun.core.repository.TagRepository;
import com.gentics.cailun.test.Neo4jSpringTestConfiguration;

@ContextConfiguration(classes = { Neo4jSpringTestConfiguration.class })
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class TagTest {

	@Autowired
	TagRepository tagRepository;

	@Autowired
	CaiLunNodeRepository<CaiLunNode> nodeRepository;

	@Test
	public void testContents() {
		final String TEST_TAG_NAME = "my node";
		CaiLunNode node1 = new CaiLunNode();
		node1.setName(TEST_TAG_NAME);
		nodeRepository.save(node1);

		Tag tag1 = new Tag();
		tag1.addContent(node1);
		tagRepository.save(tag1);

		// Reload the tag and check whether the content was set
		Tag reloadedTag = tagRepository.findOne(tag1.getId());
		assertEquals("The tag should have exactly one element.", 1, reloadedTag.getContents().size());
		CaiLunNode nodeFromTag = reloadedTag.getContents().iterator().next();
		assertEquals("The name of the loaded tag did not match the expected one.", TEST_TAG_NAME, nodeFromTag.getName());
		assertTrue(reloadedTag.removeContent(nodeFromTag));
		tagRepository.save(reloadedTag);

		// Reload again
		reloadedTag = tagRepository.findOne(tag1.getId());
		assertEquals("The tag should not have any element.", 0, reloadedTag.getContents().size());

	}
}
