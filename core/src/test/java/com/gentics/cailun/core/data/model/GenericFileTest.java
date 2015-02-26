package com.gentics.cailun.core.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.data.model.generic.GenericFile;
import com.gentics.cailun.core.data.service.generic.GenericFileService;
import com.gentics.cailun.test.AbstractDBTest;
import com.gentics.cailun.test.DummyDataProvider;

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
		GenericFile file = fileService.findByPath(DummyDataProvider.PROJECT_NAME, "/subtag/english.html");
		assertNotNull("A file within the given path should be found.", file);

		
		// Check whether we can load the english content of the found file
		Content content = (Content) file;
		Language english = getDataProvider().getEnglish();
		assertEquals("The content of the found file did not match the expected one.", DummyDataProvider.ENGLISH_CONTENT, content.getContent(english));
	}
}
