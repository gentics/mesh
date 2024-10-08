package com.gentics.mesh.cli;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.TestSize.PROJECT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.core.data.HibLanguage;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.etc.LanguageEntry;
import com.gentics.mesh.etc.LanguageSet;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

@MeshTestSetting(testSize = PROJECT, startServer = false)
public class BootstrapInitializerTest extends AbstractMeshTest {

	@Test
	public void testInitLanguages() throws JsonParseException, JsonMappingException, IOException {
		try (Tx tx = tx()) {
			boot().initLanguages();
		}
		final String filename = "languages.json";
		final InputStream ins = getClass().getResourceAsStream("/json/" + filename);
		if (ins == null) {
			throw new NullPointerException("Languages could not be loaded from classpath file {" + filename + "}");
		}
		LanguageSet languageSet = new ObjectMapper().readValue(ins, LanguageSet.class);
		for (Map.Entry<String, LanguageEntry> entry : languageSet.entrySet()) {
			try (Tx tx = tx()) {
				HibLanguage language = tx.languageDao().findByLanguageTag(entry.getKey());
				assertNotNull(language);
				assertEquals(language.getName(), entry.getValue().getName());
				assertEquals(language.getNativeName(), entry.getValue().getNativeName());
			}
		}
	}

	@Test
	public void testInitCustomLanguages() throws JsonParseException, JsonMappingException, IOException {
		FileUtils.copyURLToFile(getClass().getResource("/json/custom-languages.json"), new File("target/custom-languages.json"));

		try (Tx tx = tx()) {
			boot().initLanguages();

			HibLanguage language = tx.languageDao().findByLanguageTag("de");
			assertThat(language).as("Default language").isNotNull().hasTag("de").hasName("German").hasNativeName("Deutsch");
			tx.success();
		}

		MeshOptions configuration = options();
		configuration.setLanguagesFilePath("target/custom-languages.json");
		boot().initOptionalLanguages(configuration);

		try (Tx tx = tx()) {
			// check added language
			HibLanguage language = tx.languageDao().findByLanguageTag("sq-KS");
			assertThat(language).as("Custom language").isNotNull().hasTag("sq-KS").hasName("Albanian (Kosovo)").hasNativeName("Shqip (Kosovo)");

			// check overwritten language
			language = tx.languageDao().findByLanguageTag("de");
			assertThat(language).as("Overwritten default language").isNotNull().hasTag("de").hasName("German (modified)").hasNativeName("Deutsch (modifiziert)");

			tx.success();
		}
	}

	@Test
	public void testIndexLookup() {
		try (Tx tx = tx()) {
			boot().initDatabaseTypes();;
			boot().clearReferences();
			assertNotNull(boot().rootResolver());
		}
	}

	@Test
	public void testIsEmpty() {
		assertFalse(boot().isEmptyInstallation());
		db().clear();
		assertTrue(boot().isEmptyInstallation());
	}
}
