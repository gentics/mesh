package com.gentics.cailun.core.rest.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.repository.GlobalTagRepository;
import com.gentics.cailun.core.repository.GlobalTaggableNodeRepository;
import com.gentics.cailun.test.Neo4jSpringTestConfiguration;

@ContextConfiguration(classes = { Neo4jSpringTestConfiguration.class })
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class TaggableNodeTest {

	@Autowired
	GlobalTaggableNodeRepository<TaggableNode> taggableNodeRepository;

	@Autowired
	GlobalTagRepository tagRepository;

	@Test
	public void testNodeTagging() {
		final String TEST_TAG_NAME = "testTag";
		TaggableNode node = new TaggableNode();
		Tag tag = node.tag(TEST_TAG_NAME);
		assertNotNull("The tag method should return the created tag", tag);
		taggableNodeRepository.save(node);

		TaggableNode reloadedNode = taggableNodeRepository.findOne(node.getId());
		assertNotNull("The node shoule be loaded", reloadedNode);
		assertTrue("The test node should have a tag with the name {" + TEST_TAG_NAME + "}.", reloadedNode.hasTag(tag));

		Tag extraTag = new Tag("blub");
		assertFalse("The test node should have the random created tag.", reloadedNode.hasTag(extraTag));

		assertTrue("The tag should be removed.", reloadedNode.unTag(tag));
	}

	@Test
	public void testNodeProperties() {
		final String TEST_PROPERTY_KEY = "myProperty";
		final String TEST_PROPERTY_VALUE = "myValue";
		TaggableNode node = new TaggableNode();
		node.addProperty(TEST_PROPERTY_KEY, TEST_PROPERTY_VALUE);
		taggableNodeRepository.save(node);
		CaiLunNode reloadedNode = taggableNodeRepository.findOne(node.getId());
		assertEquals("The node should have the property", TEST_PROPERTY_VALUE, reloadedNode.getProperty(TEST_PROPERTY_KEY));
		assertTrue("The property must be removed.", reloadedNode.removeProperty(TEST_PROPERTY_KEY));
		assertFalse("The property was already removed and removing it again must fail", reloadedNode.removeProperty(TEST_PROPERTY_KEY));
		reloadedNode = taggableNodeRepository.findOne(node.getId());
		assertFalse("The node should no longer have property identified by the key", reloadedNode.hasProperty(TEST_PROPERTY_KEY));
	}
}
