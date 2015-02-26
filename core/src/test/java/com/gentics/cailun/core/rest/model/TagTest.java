package com.gentics.cailun.core.rest.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.transaction.NotSupportedException;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.data.model.Content;
import com.gentics.cailun.core.data.model.Language;
import com.gentics.cailun.core.data.model.Tag;
import com.gentics.cailun.core.data.model.generic.GenericFile;
import com.gentics.cailun.core.data.service.ContentService;
import com.gentics.cailun.core.data.service.TagService;
import com.gentics.cailun.test.AbstractDBTest;

public class TagTest extends AbstractDBTest {

	public static final String GERMAN_NAME = "test german name";

	public static final String ENGLISH_NAME = "test english name";

	@Autowired
	private TagService tagService;

	@Autowired
	private ContentService contentService;

	@Before
	public void setup() {
		setupData();
	}

	@Test
	public void testLocalizedFolder() {
		Language german = languageService.findByName("german");

		Tag tag = new Tag();
		tagService.setName(tag, german, GERMAN_NAME);
		tagService.save(tag);
		assertNotNull(tag.getId());
		tag = tagService.findOne(tag.getId());
		assertNotNull("The folder could not be found.", tag);
		assertEquals("The loaded name of the folder did not match the expected one.", GERMAN_NAME, tag.getName(german));
	}

	@Test
	public void testContents() throws NotSupportedException {

		Tag tag = new Tag();

		Language english = languageService.findByName("english");

		tagService.setName(tag, english, ENGLISH_NAME);
		tagService.save(tag);
		tag = tagService.findOne(tag.getId());
		assertNotNull(tag);

		final String GERMAN_TEST_FILENAME = "german.html";
		Content content = new Content();

		Language german = languageService.findByName("german");

		contentService.setFilename(content, german, GERMAN_TEST_FILENAME);
		contentService.setName(content, german, "german content name");
		contentService.save(content);

		tag.addFile(content);
		tagService.save(tag);

		// Reload the tag and check whether the content was set
		tag = tagService.findOne(tag.getId());
		assertEquals("The tag should have exactly one file.", 1, tag.getFiles().size());
		GenericFile fileFromTag = tag.getFiles().iterator().next();
		assertNotNull(fileFromTag);
		assertEquals("The name of the file from the loaded tag did not match the expected one.", GERMAN_TEST_FILENAME,
				fileFromTag.getFilename(german));

		// Remove the file/content and check whether the content was really removed
		assertTrue(tag.removeFile(fileFromTag));
		tagService.save(tag);
		tag = tagService.findOne(tag.getId());
		assertEquals("The tag should not have any file.", 0, tag.getFiles().size());

	}

	@Test
	public void testNodeTagging() {
		Language german = languageService.findByName("german");

		// Create root with subfolder
		final String TEST_TAG_NAME = "testTag";

		Tag rootTag = new Tag();
		tagService.setName(rootTag, german, "wurzelordner");

		Tag subFolderTag = new Tag();
		tagService.setName(subFolderTag, german, TEST_TAG_NAME);
		subFolderTag = tagService.save(subFolderTag);

		rootTag.addTag(subFolderTag);
		tagService.save(rootTag);

		Tag reloadedNode = tagService.findOne(rootTag.getId());
		assertNotNull("The node shoule be loaded", reloadedNode);
		assertTrue("The test node should have a tag with the name {" + TEST_TAG_NAME + "}.", reloadedNode.hasTag(subFolderTag));

		Tag extraTag = new Tag();
		tagService.setName(extraTag, german, "extra ordner");
		assertFalse("The test node should have the random created tag.", reloadedNode.hasTag(extraTag));

		assertTrue("The tag should be removed.", reloadedNode.removeTag(subFolderTag));
	}

	@Test
	@Transactional
	public void testNodeProperties() {
		final String TEST_PROPERTY_KEY = "myProperty";
		final String TEST_PROPERTY_VALUE = "myValue";
		Tag tag = new Tag();
		tag.setProperty(TEST_PROPERTY_KEY, TEST_PROPERTY_VALUE);
		tagService.save(tag);
		Tag reloadedTag = tagService.findOne(tag.getId());
		assertEquals("The node should have the property", TEST_PROPERTY_VALUE, reloadedTag.getProperty(TEST_PROPERTY_KEY));
		assertTrue("The property must be removed.", reloadedTag.removeProperty(TEST_PROPERTY_KEY));
		assertFalse("The property was already removed and removing it again must fail", reloadedTag.removeProperty(TEST_PROPERTY_KEY));
		tag = tagService.findOne(tag.getId());
		assertFalse("The node should no longer have property identified by the key", reloadedTag.hasProperty(TEST_PROPERTY_KEY));
	}
}
