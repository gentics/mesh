package com.gentics.mesh.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.model.Language;
import com.gentics.mesh.core.repository.LanguageRepository;
import com.gentics.mesh.test.AbstractDBTest;

public class BootstrapInitializerTest extends AbstractDBTest {

	@Autowired
	private BootstrapInitializer bootstrapInitializer;

	@Autowired
	private LanguageRepository languageRepository;

	@Before
	public void setup() throws Exception {
		setupData();
	}

	@Test
	public void testInitLanguages() throws JsonParseException, JsonMappingException, IOException {
		bootstrapInitializer.initLanguages(data().getMeshRoot());
		Language language = languageRepository.findByLanguageTag("xh");
		assertNotNull(language);
		assertEquals("Xhosa", language.getName());
		assertEquals("isiXhosa", language.getNativeName());
	}
}
