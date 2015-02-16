package com.gentics.cailun.core.rest.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.repository.GlobalCaiLunNodeRepository;
import com.gentics.cailun.core.rest.facade.FolderTagFacade;
import com.gentics.cailun.test.AbstractDBTest;

public class TagTest extends AbstractDBTest {

	// @Autowired
	// GlobalFolderTagRepository folderTagRepository;

	@Autowired
	GlobalCaiLunNodeRepository<FolderTag> folderTagRepository;

	@Autowired
	FolderTagFacade f;

	@Test
	public void testFolderTagFacade() {
		final String germanName = "test german";
		f.setName(german, germanName);
		folderTagRepository.save(f);
		f = (FolderTagFacade) folderTagRepository.findOne(f.getId());
		assertNotNull(f);
		assertEquals(germanName, f.getName(german));
	}

	@Test
	public void testLocalizedFolder() {
		final String germanName = "test german";
		FolderTag folder = new FolderTag(german, germanName);
		folderTagRepository.save(folder);
		folder = folderTagRepository.findOne(folder.getId());
		assertNotNull("The localized folder could not be loaded.", folder);
		assertEquals("The name of the localized folder did not match.", germanName, folder.getName(german));
	}

	@Test
	public void testContents() {

		// FolderTag tag = new FolderTag(german, "german");
		// LocalizedFolderTag englishTag = new LocalizedFolderTag(english, "english");
		// localizedFolderTagRepository.save(englishTag);
		// tag.addLocalization(englishTag);
		// englishTag = localizedFolderTagRepository.findOne(englishTag.getId());
		// assertNotNull(englishTag);
		// assertNotNull(englishTag.getLanguage());
		//
		// folderTagRepository.save(tag);
		//
		// tag = folderTagRepository.findOne(tag.getId());
		// assertEquals("The tag should have a german and an english localisation.", 2, tag.getLocalizations().size());
		// assertNotNull("The folder tag should have a german localisation.", tag.getLocalisation(german));

		// final String TEST_TAG_NAME = "my node";
		// CaiLunNode node1 = new CaiLunNode();
		// node1.setName(TEST_TAG_NAME);
		// nodeRepository.save(node1);
		//
		// LocalizedTag tag1 = new LocalizedTag();
		// tag1.addContent(node1);
		// tagRepository.save(tag1);
		//
		// // Reload the tag and check whether the content was set
		// LocalizedTag reloadedTag = tagRepository.findOne(tag1.getId());
		// assertEquals("The tag should have exactly one element.", 1, reloadedTag.getContents().size());
		// CaiLunNode nodeFromTag = reloadedTag.getContents().iterator().next();
		// assertEquals("The name of the loaded tag did not match the expected one.", TEST_TAG_NAME, nodeFromTag.getName());
		// assertTrue(reloadedTag.removeContent(nodeFromTag));
		// tagRepository.save(reloadedTag);
		//
		// // Reload again
		// reloadedTag = tagRepository.findOne(tag1.getId());
		// assertEquals("The tag should not have any element.", 0, reloadedTag.getContents().size());

	}

	@Test
	public void testNodeTagging() {
		final String TEST_TAG_NAME = "testTag";
		Tag node = new Tag();
		// LocalizedTag tag = node.tag(TEST_TAG_NAME);
		// assertNotNull("The tag method should return the created tag", tag);
		// tagRepository.save(node);
		//
		// Tag reloadedNode = tagRepository.findOne(node.getId());
		// assertNotNull("The node shoule be loaded", reloadedNode);
		// assertTrue("The test node should have a tag with the name {" + TEST_TAG_NAME + "}.", reloadedNode.hasTag(tag));
		//
		// Tag extraTag = new Tag("blub");
		// assertFalse("The test node should have the random created tag.", reloadedNode.hasTag(extraTag));
		//
		// assertTrue("The tag should be removed.", reloadedNode.unTag(tag));
	}

	@Test
	public void testNodeProperties() {
		final String TEST_PROPERTY_KEY = "myProperty";
		final String TEST_PROPERTY_VALUE = "myValue";
		Tag node = new Tag();
		node.addProperty(TEST_PROPERTY_KEY, TEST_PROPERTY_VALUE);
		// tagRepository.save(node);
		// CaiLunNode reloadedNode = tagRepository.findOne(node.getId());
		// assertEquals("The node should have the property", TEST_PROPERTY_VALUE, reloadedNode.getProperty(TEST_PROPERTY_KEY));
		// assertTrue("The property must be removed.", reloadedNode.removeProperty(TEST_PROPERTY_KEY));
		// assertFalse("The property was already removed and removing it again must fail", reloadedNode.removeProperty(TEST_PROPERTY_KEY));
		// reloadedNode = tagRepository.findOne(node.getId());
		// assertFalse("The node should no longer have property identified by the key", reloadedNode.hasProperty(TEST_PROPERTY_KEY));
	}
}
