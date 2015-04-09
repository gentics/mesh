package com.gentics.cailun.core.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.repository.LanguageRepository;
import com.gentics.cailun.test.AbstractDBTest;

@Transactional
public class LanguageTest extends AbstractDBTest {

	@Autowired
	LanguageRepository languageRepository;

	@Before
	public void setup() throws Exception {
		setupData();
	}

	@Test
	public void testCreation() {
		final String languageName = "klingon";
		final String languageTag = "tlh";
		Language lang = new Language(languageName, languageTag);
		languageRepository.save(lang);
		lang = languageRepository.findOne(lang.getId());
		assertNotNull(lang);
		assertEquals(languageName, lang.getName());

		assertNotNull(languageRepository.findByLanguageTag(languageTag));
	}

	@Test
	public void testLanguageRoot() {
		int nLanguagesBefore = languageRepository.findRoot().getLanguages().size();
		final String languageName = "klingon";
		final String languageTag = "tlh";
		Language lang = new Language(languageName, languageTag);

		languageRepository.save(lang);

		int nLanguagesAfter = languageRepository.findRoot().getLanguages().size();
		assertEquals(nLanguagesBefore + 1, nLanguagesAfter);

	}

}
