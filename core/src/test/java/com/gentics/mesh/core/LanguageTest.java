package com.gentics.mesh.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.root.LanguageRoot;
import com.gentics.mesh.core.data.service.BasicObjectTestcases;
import com.gentics.mesh.core.data.service.LanguageService;
import com.gentics.mesh.test.AbstractDBTest;
import com.gentics.mesh.util.InvalidArgumentException;

public class LanguageTest extends AbstractDBTest implements BasicObjectTestcases {

	@Autowired
	private LanguageService languageService;

	@Before
	public void setup() throws Exception {
		setupData();
	}

	@Test
	@Override
	public void testRootNode() {
		LanguageRoot languageRoot = data().getMeshRoot().getLanguageRoot();

		int nLanguagesBefore = languageRoot.getLanguages().size();

		final String languageName = "klingon";
		final String languageTag = "tlh";
		Language lang = languageRoot.create(languageName, languageTag);

		int nLanguagesAfter = languageRoot.getLanguages().size();
		assertEquals(nLanguagesBefore + 1, nLanguagesAfter);

	}

	@Test
	@Override
	public void testFindAllVisible() throws InvalidArgumentException {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testFindAll() throws InvalidArgumentException {
		List<? extends Language> languages = languageService.findAll();
		assertEquals(182, languages.size());

	}

	@Test
	@Override
	public void testFindByName() {
		Language language = languageService.findByName("German");
		assertNotNull(language);
		assertEquals("German", language.getName());
		assertEquals("Deutsch", language.getNativeName());
		assertEquals("de", language.getLanguageTag());

		language = languageService.findByName("bogus");
		assertNull(language);

	}

	@Test
	@Override
	public void testFindByUUID() {
		Language language = languageService.findByName("German");

		Language foundLanguage = languageService.findByUUID(language.getUuid());
		assertNotNull(foundLanguage);

		foundLanguage = languageService.findByUUID("bogus");
		assertNull(foundLanguage);

	}

	@Test
	@Override
	@Ignore("Languages are not transformable to rest")
	public void testTransformation() {
	}

	@Test
	@Override
	public void testCreateDelete() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testCRUDPermissions() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testPermissionsOnObject() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testRead() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testCreate() {
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
	@Override
	public void testDelete() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testUpdate() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testReadPermission() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testDeletePermission() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testUpdatePermission() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testCreatePermission() {
		// TODO Auto-generated method stub

	}

}
