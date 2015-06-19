package com.gentics.mesh.core.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.model.tinkerpop.Language;
import com.gentics.mesh.core.data.model.tinkerpop.Tag;
import com.gentics.mesh.core.data.service.LanguageService;
import com.gentics.mesh.core.data.service.TagService;
import com.gentics.mesh.test.AbstractDBTest;

public class AtomicTagTest extends AbstractDBTest {

	@Autowired
	private TagService tagService;

	@Autowired
	private LanguageService languageService;

	@Test
	public void testTagCreation() {
		Language language = languageService.create("Deutsch", "de");
		Tag tag = tagService.create();
		assertNotNull(tag);
		tag.setContent(language, "test content");

		Tag reloadedTag = tagService.findByUUID(tag.getUuid());
		assertNotNull(reloadedTag);
		assertNotNull(reloadedTag.getI18nProperties());
		assertEquals(1, reloadedTag.getI18nProperties().size());
	}
}
