package com.gentics.mesh.core.field.microschema;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.test.AbstractDBTest;

public class MicroschemaGraphFieldNodeTest extends AbstractDBTest {

	@Autowired
	private ServerSchemaStorage schemaStorage;

	@Before
	public void setup() throws Exception {
		setupData();
	}

	@Test
	@Ignore("Not yet implemented")
	public void testMicroschemaFieldTransformation() {

	}
}
