package com.gentics.mesh.core;

import static com.gentics.mesh.util.MeshAssert.failingLatch;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.LanguageRoot;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.test.AbstractBasicObjectTest;
import com.gentics.mesh.util.InvalidArgumentException;

public class LanguageTest extends AbstractBasicObjectTest {

	@Test
	@Override
	public void testRootNode() {
		LanguageRoot languageRoot = meshRoot().getLanguageRoot();

		int nLanguagesBefore = languageRoot.findAll().size();

		final String languageName = "klingon";
		final String languageTag = "tlh";
		assertNotNull(languageRoot.create(languageName, languageTag));

		int nLanguagesAfter = languageRoot.findAll().size();
		assertEquals(nLanguagesBefore + 1, nLanguagesAfter);
	}

	@Test
	@Override
	@Ignore("Not yet implemented")
	public void testFindAllVisible() throws InvalidArgumentException {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testFindAll() throws InvalidArgumentException {
		List<? extends Language> languages = meshRoot().getLanguageRoot().findAll();
		assertEquals(4, languages.size());
	}

	@Test
	@Override
	public void testFindByName() {
		Language language = meshRoot().getLanguageRoot().findByName("German");
		assertNotNull(language);
		assertEquals("German", language.getName());
		assertEquals("Deutsch", language.getNativeName());
		assertEquals("de", language.getLanguageTag());

		language = meshRoot().getLanguageRoot().findByName("bogus");
		assertNull(language);
	}

	@Test
	@Override
	public void testFindByUUID() throws InterruptedException {
		Language language = meshRoot().getLanguageRoot().findByName("German");

		CountDownLatch latch = new CountDownLatch(2);
		meshRoot().getLanguageRoot().findByUuid(language.getUuid(), rh -> {
			Language foundLanguage = rh.result();
			assertNotNull(foundLanguage);
			latch.countDown();
		});

		meshRoot().getLanguageRoot().findByUuid("bogus", rh -> {
			Language foundLanguage = rh.result();
			assertNull(foundLanguage);
			latch.countDown();
		});
		failingLatch(latch);
	}

	@Test
	@Override
	@Ignore("languages are currently not transformable")
	public void testTransformation() {
	}

	@Test
	@Override
	@Ignore("Languages can not be dynamically created")
	public void testCreateDelete() {
	}

	@Test
	@Override
	@Ignore("Languages can not be dynamically created")
	public void testCRUDPermissions() {
	}

	@Test
	@Override
	public void testRead() {
		Language language = english();
		assertNotNull(language.getName());
		assertEquals("English", language.getName());
		assertNotNull(language.getNativeName());
		assertEquals("English", language.getNativeName());
		assertNotNull(language.getLanguageTag());
		assertEquals("en", language.getLanguageTag());
	}

	@Test
	@Override
	public void testCreate() {
		LanguageRoot languageRoot = meshRoot().getLanguageRoot();
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
	@Ignore("Languages can not be deleted")
	public void testDelete() {
	}

	@Test
	@Override
	@Ignore("Languages can not be updated")
	public void testUpdate() {
	}

	@Test
	@Override
	public void testReadPermission() {
		testPermission(GraphPermission.READ_PERM, english());
	}

	@Test
	@Override
	public void testDeletePermission() {
		testPermission(GraphPermission.DELETE_PERM, english());
	}

	@Test
	@Override
	public void testUpdatePermission() {
		testPermission(GraphPermission.UPDATE_PERM, english());
	}

	@Test
	@Override
	public void testCreatePermission() {
		testPermission(GraphPermission.CREATE_PERM, english());
	}

}
