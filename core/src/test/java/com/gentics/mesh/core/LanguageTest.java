package com.gentics.mesh.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.root.LanguageRoot;
import com.gentics.mesh.test.AbstractBasicObjectTest;
import com.gentics.mesh.util.InvalidArgumentException;

public class LanguageTest extends AbstractBasicObjectTest {

	private LanguageRoot languageRoot = boot.languageRoot();

	@Test
	@Override
	public void testRootNode() {
		LanguageRoot languageRoot = data().getMeshRoot().getLanguageRoot();

		int nLanguagesBefore = languageRoot.findAll().size();

		final String languageName = "klingon";
		final String languageTag = "tlh";
		Language lang = languageRoot.create(languageName, languageTag);

		int nLanguagesAfter = languageRoot.findAll().size();
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
		List<? extends Language> languages = languageRoot.findAll();
		assertEquals(182, languages.size());

	}

	@Test
	@Override
	public void testFindByName() {
		Language language = languageRoot.findByName("German");
		assertNotNull(language);
		assertEquals("German", language.getName());
		assertEquals("Deutsch", language.getNativeName());
		assertEquals("de", language.getLanguageTag());

		language = languageRoot.findByName("bogus");
		assertNull(language);

	}

	@Test
	@Override
	public void testFindByUUID() {
		Language language = languageRoot.findByName("German");

		Language foundLanguage = languageRoot.findByUUID(language.getUuid());
		assertNotNull(foundLanguage);

		foundLanguage = languageRoot.findByUUID("bogus");
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

		lang = languageRoot.findByName(languageName);
		assertNotNull(lang);
		assertEquals(languageName, lang.getName());

		assertNotNull(languageRoot.findByLanguageTag(languageTag));
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
