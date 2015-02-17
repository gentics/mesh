package com.gentics.cailun.core.rest.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.transaction.NotSupportedException;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.repository.GlobalContentRepository;
import com.gentics.cailun.core.repository.GlobalFolderTagRepository;
import com.gentics.cailun.core.rest.service.ContentService;
import com.gentics.cailun.core.rest.service.FolderTagService;
import com.gentics.cailun.test.AbstractDBTest;

public class FolderTagTest extends AbstractDBTest {

	public static final String GERMAN_NAME = "test german name";

	public static final String ENGLISH_NAME = "test english name";

	@Autowired
	private FolderTagService folderTagService;

	@Autowired
	private GlobalFolderTagRepository folderTagRepository;

	@Autowired
	private ContentService contentService;

	@Autowired
	private GlobalContentRepository contentRepository;

	@Test
	public void testLocalizedFolder() {
		FolderTag folderTag = new FolderTag();
		folderTagService.setName(folderTag, german, GERMAN_NAME);
		folderTagRepository.save(folderTag);
		assertNotNull(folderTag.getId());
		folderTag = folderTagRepository.findOne(folderTag.getId());
		assertNotNull("The folder could not be found.", folderTag);
		assertEquals("The loaded name of the folder did not match the expected one.", GERMAN_NAME, folderTag.getName(german));
	}

	@Test
	public void testContents() throws NotSupportedException {

		FolderTag tag = new FolderTag();
		folderTagService.setName(tag, english, ENGLISH_NAME);
		folderTagRepository.save(tag);
		tag = folderTagRepository.findOne(tag.getId());
		assertNotNull(tag);

		final String GERMAN_TEST_FILENAME = "german.html";
		Content content = new Content();

		contentService.setFilename(content, german, GERMAN_TEST_FILENAME);
		contentService.setName(content, german, "german content name");

		contentRepository.save(content);
		tag.addFile(content);
		folderTagRepository.save(tag);

		// Reload the tag and check whether the content was set
		tag = folderTagRepository.findOne(tag.getId());
		assertEquals("The tag should have exactly one file.", 1, tag.getFiles().size());
		File fileFromTag = tag.getFiles().iterator().next();
		assertNotNull(fileFromTag);
		assertEquals("The name of the file from the loaded tag did not match the expected one.", GERMAN_TEST_FILENAME,
				fileFromTag.getFilename(german));

		// Remove the file/content and check whether the content was really removed
		assertTrue(tag.removeFile(fileFromTag));
		folderTagRepository.save(tag);
		tag = folderTagRepository.findOne(tag.getId());
		assertEquals("The tag should not have any file.", 0, tag.getFiles().size());

	}

	@Test
	public void testNodeTagging() {
		// Create root with subfolder
		final String TEST_TAG_NAME = "testTag";

		FolderTag rootTag = new FolderTag();
		folderTagService.setName(rootTag, german, "wurzelordner");

		FolderTag subFolderTag = new FolderTag();
		folderTagService.setName(subFolderTag, german, TEST_TAG_NAME);
		subFolderTag = folderTagRepository.save(subFolderTag);

		rootTag.addTag(subFolderTag);
		folderTagRepository.save(rootTag);

		FolderTag reloadedNode = folderTagRepository.findOne(rootTag.getId());
		assertNotNull("The node shoule be loaded", reloadedNode);
		assertTrue("The test node should have a tag with the name {" + TEST_TAG_NAME + "}.", reloadedNode.hasTag(subFolderTag));

		FolderTag extraTag = new FolderTag();
		folderTagService.setName(extraTag, german, "extra ordner");
		assertFalse("The test node should have the random created tag.", reloadedNode.hasTag(extraTag));

		assertTrue("The tag should be removed.", reloadedNode.removeTag(subFolderTag));
	}

	@Test
	public void testNodeProperties() {
		final String TEST_PROPERTY_KEY = "myProperty";
		final String TEST_PROPERTY_VALUE = "myValue";
		FolderTag tag = new FolderTag();
		tag.addProperty(TEST_PROPERTY_KEY, TEST_PROPERTY_VALUE);
		folderTagRepository.save(tag);
		FolderTag reloadedTag = folderTagRepository.findOne(tag.getId());
		assertEquals("The node should have the property", TEST_PROPERTY_VALUE, reloadedTag.getProperty(TEST_PROPERTY_KEY));
		assertTrue("The property must be removed.", reloadedTag.removeProperty(TEST_PROPERTY_KEY));
		assertFalse("The property was already removed and removing it again must fail", reloadedTag.removeProperty(TEST_PROPERTY_KEY));
		tag = folderTagRepository.findOne(tag.getId());
		assertFalse("The node should no longer have property identified by the key", reloadedTag.hasProperty(TEST_PROPERTY_KEY));
	}
}
