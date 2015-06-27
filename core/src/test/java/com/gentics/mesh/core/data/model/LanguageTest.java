package com.gentics.mesh.core.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.model.root.LanguageRoot;
import com.gentics.mesh.core.data.model.root.impl.LanguageRootImpl;
import com.gentics.mesh.core.data.service.LanguageService;
import com.gentics.mesh.test.AbstractDBTest;

public class LanguageTest extends AbstractDBTest {

	@Autowired
	private LanguageService languageService;

	@Before
	public void setup() throws Exception {
		setupData();
	}

	@Test
	public void testCreation() {
		LanguageRoot languageRoot = data().getMeshRoot().getLanguageRoot();
		final String languageTag = "tlh";
		final String languageName = "klingon";
		Language lang = languageRoot.create(languageName, languageTag);

		lang = languageService.findByName(languageName);
		assertNotNull(lang);
		assertEquals(languageName, lang.getName());

		assertNotNull(languageService.findByLanguageTag(languageTag));
	}

	@Test
	public void testLanguageRoot() {
		LanguageRoot languageRoot = data().getMeshRoot().getLanguageRoot();

		int nLanguagesBefore = languageRoot.getLanguages().size();

		final String languageName = "klingon";
		final String languageTag = "tlh";
		Language lang = languageRoot.create(languageName, languageTag);

		int nLanguagesAfter = languageRoot.getLanguages().size();
		assertEquals(nLanguagesBefore + 1, nLanguagesAfter);

	}

}
