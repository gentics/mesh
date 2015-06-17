package com.gentics.mesh.core.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.model.tinkerpop.Language;
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
		final String languageTag = "tlh";
		final String languageName = "klingon";
		Language lang = languageService.create(languageName, languageTag);
//		try (Transaction tx = graphDb.beginTx()) {
//			tx.success();
//		}
		//TODO reload?
		lang = null;
		assertNotNull(lang);
		assertEquals(languageName, lang.getName());

		assertNotNull(languageService.findByLanguageTag(languageTag));
	}

	@Test
	public void testLanguageRoot() {
		int nLanguagesBefore = languageService.findRoot().getLanguages().size();

//		try (Transaction tx = graphDb.beginTx()) {
			final String languageName = "klingon";
			final String languageTag = "tlh";
			Language lang = languageService.create(languageName, languageTag);
//			tx.success();
//		}

		int nLanguagesAfter = languageService.findRoot().getLanguages().size();
		assertEquals(nLanguagesBefore + 1, nLanguagesAfter);

	}

}
