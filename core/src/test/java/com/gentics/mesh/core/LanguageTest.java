package com.gentics.mesh.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.root.LanguageRoot;
import com.gentics.mesh.core.data.service.LanguageService;
import com.gentics.mesh.test.AbstractBasicObjectTest;
import com.gentics.mesh.util.InvalidArgumentException;

public class LanguageTest extends AbstractBasicObjectTest {

	@Autowired
	private LanguageService languageService;

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
		fail("Not yet implemented");
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
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testCRUDPermissions() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testPermissionsOnObject() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testRead() {
		fail("Not yet implemented");
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
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testUpdate() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testReadPermission() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testDeletePermission() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testUpdatePermission() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testCreatePermission() {
		fail("Not yet implemented");
	}

}
