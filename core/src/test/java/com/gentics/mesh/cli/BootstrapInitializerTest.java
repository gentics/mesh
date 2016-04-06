package com.gentics.mesh.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.core.data.AbstractBasicDBTest;
import com.gentics.mesh.core.data.Language;

public class BootstrapInitializerTest extends AbstractBasicDBTest {

	@Test
	public void testInitLanguages() throws JsonParseException, JsonMappingException, IOException {
		boot.initLanguages(meshRoot().getLanguageRoot());
		Language language = boot.languageRoot().findByLanguageTag("de");
		assertNotNull(language);
		assertEquals("German", language.getName());
		assertEquals("Deutsch", language.getNativeName());
	}
}
