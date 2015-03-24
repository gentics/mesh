package com.gentics.cailun.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.cailun.core.data.model.Language;
import com.gentics.cailun.core.repository.LanguageRepository;
import com.gentics.cailun.test.AbstractDBTest;

public class BootstrapInitializerTest extends AbstractDBTest {

	@Autowired
	private BootstrapInitializer bootstrapInitializer;

	@Autowired
	private LanguageRepository languageRepository;

	@Before
	public void setup() {
		setupData();
	}

	@Test
	@Transactional
	public void testInitLanguages() throws JsonParseException, JsonMappingException, IOException {
		bootstrapInitializer.initLanguages(data().getCaiLunRoot());
		Language language = languageRepository.findByLanguageTag("xh");
		assertNotNull(language);
		assertEquals("Xhosa", language.getName());
		assertEquals("isiXhosa", language.getNativeName());
	}
}
