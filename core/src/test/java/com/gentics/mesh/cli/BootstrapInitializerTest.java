package com.gentics.mesh.cli;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.TestSize.PROJECT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.core.data.HibLanguage;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = PROJECT, startServer = false)
public class BootstrapInitializerTest extends AbstractMeshTest {

	@Test
	public void testInitLanguages() throws JsonParseException, JsonMappingException, IOException {
		try (Tx tx = tx()) {
			boot().initLanguages(meshRoot().getLanguageRoot());
			HibLanguage language = tx.languageDao().findByLanguageTag("de");
			assertNotNull(language);
			assertEquals("German", language.getName());
			assertEquals("Deutsch", language.getNativeName());
		}
	}

	@Test
	public void testInitCustomLanguages() throws JsonParseException, JsonMappingException, IOException {
		FileUtils.copyURLToFile(getClass().getResource("/json/custom-languages.json"), new File("target/custom-languages.json"));

		try (Tx tx = tx()) {
			boot().initLanguages(meshRoot().getLanguageRoot());

			HibLanguage language = tx.languageDao().findByLanguageTag("de");
			assertThat(language).as("Default language").isNotNull().hasTag("de").hasName("German").hasNativeName("Deutsch");
			tx.success();
		}

		MeshOptions configuration = new MeshOptions();
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
			boot().meshRoot();
			boot().clearReferences();
			assertNotNull(boot().meshRoot());
		}
	}

	@Test
	public void testIsEmpty() {
		try (Tx tx = tx()) {
			assertFalse(boot().isEmptyInstallation());
			db().clear();
			assertTrue(boot().isEmptyInstallation());
		}
	}
}
