package com.gentics.mesh.core.language;

import static com.gentics.mesh.test.performance.StopWatch.stopWatch;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.impl.LanguageImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.LanguageRoot;
import com.gentics.mesh.graphdb.NoTrx;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.test.AbstractBasicIsolatedObjectTest;
import com.gentics.mesh.util.InvalidArgumentException;
import com.tinkerpop.blueprints.Vertex;

public class LanguageTest extends AbstractBasicIsolatedObjectTest {

	@Ignore("test test not apply")
	@Override
	public void testTransformToReference() throws Exception {
	}

	@Test
	@Override
	public void testRootNode() {
		try (NoTrx noTx = db.noTrx()) {
			LanguageRoot languageRoot = meshRoot().getLanguageRoot();

			int nLanguagesBefore = languageRoot.findAll().size();

			final String languageName = "klingon";
			final String languageTag = "tlh";
			assertNotNull(languageRoot.create(languageName, languageTag));

			int nLanguagesAfter = languageRoot.findAll().size();
			assertEquals(nLanguagesBefore + 1, nLanguagesAfter);
		}
	}

	@Test
	public void testLanguageIndex() {
		try (NoTrx noTx = db.noTrx()) {
			stopWatch("languageindex.read", 50000, (step) -> {
				Iterable<Vertex> it = Database.getThreadLocalGraph().getVertices("LanguageImpl.languageTag", "en");
				assertTrue(it.iterator().hasNext());
				Iterable<Vertex> it2 = Database.getThreadLocalGraph()
						.getVertices(LanguageImpl.class.getSimpleName() + "." + LanguageImpl.LANGUAGE_TAG_PROPERTY_KEY, "en");
				assertTrue(it2.iterator().hasNext());
				Vertex vertex = it2.iterator().next();
				assertNotNull("The language node with languageTag 'en' could not be found.", vertex);
				assertEquals("en", vertex.getProperty(LanguageImpl.LANGUAGE_TAG_PROPERTY_KEY));
			});
		}
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
		try (NoTrx noTx = db.noTrx()) {
			List<? extends Language> languages = meshRoot().getLanguageRoot().findAll();
			assertEquals(4, languages.size());
		}
	}

	@Test
	public void testFindByLanguageTag() {
		try (NoTrx noTx = db.noTrx()) {
			// for (int e = 0; e < 15; e++) {
			int nChecks = 50000;
			long start = System.currentTimeMillis();
			for (int i = 0; i < nChecks; i++) {
				LanguageRoot root = meshRoot().getLanguageRoot();
				Language language = root.findByLanguageTag("de");
				assertNotNull(language);
			}

			long duration = System.currentTimeMillis() - start;
			double perCheck = ((double) duration / (double) nChecks);
			System.out.println("Duration per lookup: " + perCheck);
			System.out.println("Duration: " + duration);
		}
	}

	@Test
	@Override
	public void testFindByName() {
		try (NoTrx noTx = db.noTrx()) {
			Language language = meshRoot().getLanguageRoot().findByName("German").toBlocking().value();
			assertNotNull(language);

			assertEquals("German", language.getName());
			assertEquals("Deutsch", language.getNativeName());
			assertEquals("de", language.getLanguageTag());

			language = meshRoot().getLanguageRoot().findByName("bogus").toBlocking().value();
			assertNull(language);
		}
	}

	@Test
	@Override
	public void testFindByUUID() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			Language language = meshRoot().getLanguageRoot().findByName("German").toBlocking().value();
			Language foundLanguage = meshRoot().getLanguageRoot().findByUuid(language.getUuid()).toBlocking().value();
			assertNotNull(foundLanguage);

			foundLanguage = meshRoot().getLanguageRoot().findByUuid("bogus").toBlocking().value();
			assertNull(foundLanguage);
		}

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
		try (NoTrx noTx = db.noTrx()) {
			Language language = english();
			assertNotNull(language.getName());
			assertEquals("English", language.getName());
			assertNotNull(language.getNativeName());
			assertEquals("English", language.getNativeName());
			assertNotNull(language.getLanguageTag());
			assertEquals("en", language.getLanguageTag());
		}
	}

	@Test
	@Override
	public void testCreate() {
		try (NoTrx noTx = db.noTrx()) {
			LanguageRoot languageRoot = meshRoot().getLanguageRoot();
			final String languageTag = "tlh";
			final String languageName = "klingon";
			Language lang = languageRoot.create(languageName, languageTag);

			lang = languageRoot.findByName(languageName).toBlocking().value();
			assertNotNull(lang);
			assertEquals(languageName, lang.getName());

			assertNotNull(languageRoot.findByLanguageTag(languageTag));
		}
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
		try (NoTrx noTx = db.noTrx()) {
			testPermission(GraphPermission.READ_PERM, english());
		}
	}

	@Test
	@Override
	public void testDeletePermission() {
		try (NoTrx noTx = db.noTrx()) {
			testPermission(GraphPermission.DELETE_PERM, english());
		}
	}

	@Test
	@Override
	public void testUpdatePermission() {
		try (NoTrx noTx = db.noTrx()) {
			testPermission(GraphPermission.UPDATE_PERM, english());
		}
	}

	@Test
	@Override
	public void testCreatePermission() {
		try (NoTrx noTx = db.noTrx()) {
			testPermission(GraphPermission.CREATE_PERM, english());
		}
	}

}
