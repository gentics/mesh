package com.gentics.mesh.core.data.service;

import java.io.IOException;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.test.AbstractDBTest;

public class RoutingContextServiceTest extends AbstractDBTest {

	@Autowired
	private RoutingContextService meshDatabaseService;

	@Before
	public void setup() throws JsonParseException, JsonMappingException, IOException {
		setupData();
	}

}
