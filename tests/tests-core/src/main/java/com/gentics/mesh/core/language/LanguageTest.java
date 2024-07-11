package com.gentics.mesh.core.language;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.dao.DaoTransformable;
import com.gentics.mesh.core.data.dao.LanguageDao;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.service.BasicObjectTestcases;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.lang.LanguageResponse;
import com.gentics.mesh.error.InvalidArgumentException;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.google.common.collect.Iterators;

import io.vertx.ext.web.RoutingContext;

@MeshTestSetting(testSize = TestSize.PROJECT, startServer = false)
public class LanguageTest extends AbstractMeshTest implements BasicObjectTestcases {

	@Ignore("test test not apply")
	@Override
	public void testTransformToReference() throws Exception {
	}

	public Language englishLang() {
		try (Tx tx = tx()) {
			return tx.languageDao().findByLanguageTag("en");
		}
	}

	@Test
	@Override
	public void testRootNode() {
		try (Tx tx = tx()) {
			LanguageDao languageDao = tx.languageDao();

			long nLanguagesBefore = languageDao.count();

			final String languageName = "klingon";
			final String languageTag = "tlh";
			assertNotNull(languageDao.create(languageName, languageTag));

			long nLanguagesAfter = languageDao.count();
			assertEquals(nLanguagesBefore + 1, nLanguagesAfter);
		}
	}

	/**
	 * Here we are looking for the visible fields, since languages are off the permission system.
	 */
	@Test
	@Override
	public void testFindAllVisible() throws InvalidArgumentException {
		LanguageResponse response = tx(tx -> {
			LanguageDao languageDao = tx.languageDao();
			Language english = languageDao.findByLanguageTag("en");
			InternalActionContext ac = mockActionContext();
			ac.getGenericParameters().setFields("languageTag");
			return languageDao.transformToRestSync(english, ac, 0);
		});
		assertNotNull(response.getLanguageTag());
		assertEquals(response.getLanguageTag(), "en");
		assertNull(response.getUuid());
		assertNull(response.getNativeName());
		assertNull(response.getName());
	}

	@Test
	@Override
	public void testFindAll() throws InvalidArgumentException {
		try (Tx tx = tx()) {
			long size = Iterators.size(tx.languageDao().findAll().iterator());
			assertEquals(11, size);
		}
	}

	@Test
	public void testFindByLanguageTag() {
		try (Tx tx = tx()) {
			// for (int e = 0; e < 15; e++) {
			int nChecks = 50000;
			long start = System.currentTimeMillis();
			for (int i = 0; i < nChecks; i++) {
				Language language = tx.languageDao().findByLanguageTag("de");
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
		try (Tx tx = tx()) {
			Language language = tx.languageDao().findByName("German");
			assertNotNull(language);

			assertEquals("German", language.getName());
			assertEquals("Deutsch", language.getNativeName());
			assertEquals("de", language.getLanguageTag());

			language = tx.languageDao().findByName("bogus");
			assertNull(language);
		}
	}

	@Test
	@Override
	public void testFindByUUID() throws Exception {
		try (Tx tx = tx()) {
			Language language = tx.languageDao().findByName("German");
			Language foundLanguage = tx.languageDao().findByUuid(language.getUuid());
			assertNotNull(foundLanguage);

			foundLanguage = tx.languageDao().findByUuid("bogus");
			assertNull(foundLanguage);
		}

	}

	@Test
	@Override
	public void testTransformation() {
		try (Tx tx = tx()) {
			LanguageDao languageDao = tx.languageDao();
			Language language = tx.languageDao().findByName("German");

			doTransformationTests(languageDao, language,
					Pair.of("uuid", Language::getUuid),
					Pair.of("name", Language::getName),
					Pair.of("nativeName", Language::getNativeName),
					Pair.of("languageTag", Language::getLanguageTag));
	}
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
		try (Tx tx = tx()) {
			Language language = englishLang();
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
		try (Tx tx = tx()) {
			LanguageDao languageDao = tx.languageDao();
			final String languageTag = "tlh";
			final String languageName = "klingon";
			Language lang = languageDao.create(languageName, languageTag);

			lang = languageDao.findByName(languageName);
			assertNotNull(lang);
			assertEquals(languageName, lang.getName());

			assertNotNull(languageDao.findByLanguageTag(languageTag));
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
		testPermission(InternalPermission.READ_PERM, englishLang());
	}

	@Test
	@Override
	public void testDeletePermission() {
		testPermission(InternalPermission.DELETE_PERM, englishLang());
	}

	@Test
	@Override
	public void testUpdatePermission() {
		testPermission(InternalPermission.UPDATE_PERM, englishLang());
	}

	@Test
	@Override
	public void testCreatePermission() {
		testPermission(InternalPermission.CREATE_PERM, englishLang());
	}

}
