package com.gentics.mesh.cli;

import static com.gentics.mesh.test.TestSize.PROJECT;
import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = false, testSize = PROJECT, startServer = false)
public class BootstrapInitializerTest extends AbstractMeshTest {

	@Test
	public void testInitLanguages() throws JsonParseException, JsonMappingException, IOException {
		try (NoTx noTx = db().noTx()) {
			boot().initLanguages(meshRoot().getLanguageRoot());
			Language language = boot().languageRoot().findByLanguageTag("de");
			assertNotNull(language);
			assertEquals("German", language.getName());
			assertEquals("Deutsch", language.getNativeName());
		}
	}

	@Test
	public void testIndexLookup() {
		try (NoTx noTx = db().noTx()) {
			boot().meshRoot();
			BootstrapInitializerImpl.clearReferences();
			assertNotNull(boot().meshRoot());
		}
	}

	@Test
	public void testIsEmpty() {
		try (NoTx noTx = db().noTx()) {
			assertFalse(boot().isEmptyInstallation());
			db().clear();
			assertTrue(boot().isEmptyInstallation());
		}
	}
}
