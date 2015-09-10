package com.gentics.mesh.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.test.AbstractDBTest;

public class BootstrapInitializerTest extends AbstractDBTest {

	@Before
	public void setup() throws Exception {
		setupData();
	}

	@Test
	public void testInitLanguages() throws JsonParseException, JsonMappingException, IOException {
		try (Trx tx = db.trx()) {
			boot.initLanguages(meshRoot().getLanguageRoot());
			Language language = boot.languageRoot().findByLanguageTag("de");
			assertNotNull(language);
			assertEquals("German", language.getName());
			assertEquals("Deutsch", language.getNativeName());
		}
	}
}
