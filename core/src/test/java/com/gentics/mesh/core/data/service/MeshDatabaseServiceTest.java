package com.gentics.mesh.core.data.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.core.data.model.impl.LanguageImpl;
import com.gentics.mesh.test.AbstractDBTest;

public class MeshDatabaseServiceTest extends AbstractDBTest {

	@Autowired
	private MeshDatabaseService meshDatabaseService;

	@Before
	public void setup() throws JsonParseException, JsonMappingException, IOException {
		setupData();
	}

	@Test
	public void testGraphDatabaseService() {
		LanguageImpl language = meshDatabaseService.findByUUID(data().getEnglish().getUuid(), LanguageImpl.class);
		assertNotNull(language);
		assertEquals("English", language.getName());
	}
}
