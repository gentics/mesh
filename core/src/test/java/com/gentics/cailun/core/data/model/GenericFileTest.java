package com.gentics.cailun.core.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.data.model.generic.GenericFile;
import com.gentics.cailun.core.data.service.generic.GenericFileService;
import com.gentics.cailun.demo.DemoDataProvider;
import com.gentics.cailun.test.AbstractDBTest;

public class GenericFileTest extends AbstractDBTest {

	@Autowired
	private GenericFileService<GenericFile> fileService;

	@Before
	public void setup() {
		setupData();
	}

	@Test
	@Transactional
	public void testProjectFileLocatingByPath() {

		@SuppressWarnings("rawtypes")
		GenericFile file = fileService.findByPath(DemoDataProvider.PROJECT_NAME, "/subtag/english.html");
		assertNotNull("A file within the given path should be found.", file);

		// Check whether we can load the english content of the found file
		Content content = (Content) file;
		Language english = data().getEnglish();
		assertEquals("The content of the found file did not match the expected one.", "1245", content.getContent(english));
	}

	@Test
	public void testDeleteFileByObject() {
		Content file = data().getNews2015Content();
		fileService.delete(file);
		assertNull(fileService.findOne(file.getId()));
		assertNull(fileService.findByUUID(file.getUuid()));
	}

	@Test
	public void testDeleteFileByUUID() {
		Content file = data().getNews2015Content();
		fileService.deleteByUUID(file.getUuid());
		assertNull(fileService.findOne(file.getId()));
		assertNull(fileService.findByUUID(file.getUuid()));
	}

	@Test
	public void testDeleteFileWithMissingPermission() {
		fail("Not yet implemented");
	}
}
