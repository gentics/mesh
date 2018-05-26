package com.gentics.mesh.search.index.node;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;

import io.vertx.core.json.JsonObject;

public class AttachmentIngestConfigProviderTest {

	AttachmentIngestConfigProvider provider = new AttachmentIngestConfigProvider();

	@Test
	public void testConfigGeneration() {

		// Test with no field
		Schema schema = new SchemaModelImpl();
		assertNotNull(provider.getConfig(schema));

		// String field
		schema.addField(FieldUtil.createStringFieldSchema("test1"));
		assertNotNull(provider.getConfig(schema));

		// Single field
		schema.addField(FieldUtil.createBinaryFieldSchema("test2"));
		JsonObject op = provider.getConfig(schema);
		assertEquals(1, op.getJsonArray("processors").size());

		// Two fields
		schema.addField(FieldUtil.createBinaryFieldSchema("test3"));
		JsonObject op2 = provider.getConfig(schema);
		assertEquals(2, op2.getJsonArray("processors").size());
	}
}
