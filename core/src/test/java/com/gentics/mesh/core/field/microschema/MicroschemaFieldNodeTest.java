package com.gentics.mesh.core.field.microschema;

import java.io.IOException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.test.AbstractDBTest;

public class MicroschemaFieldNodeTest extends AbstractDBTest {

	@Autowired
	private ServerSchemaStorage schemaStorage;

	@Before
	public void setup() throws JsonParseException, JsonMappingException, IOException {
		setupData();
	}

	@Test
	@Ignore("Not yet implemented")
	public void testMicroschemaFieldTransformation() {

	}
}
