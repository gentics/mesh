package com.gentics.mesh.search.index.node;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

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
		assertFalse(provider.getConfig(schema).isPresent());

		// String field
		schema.addField(FieldUtil.createStringFieldSchema("test1"));
		assertFalse(provider.getConfig(schema).isPresent());

		// Single field
		schema.addField(FieldUtil.createBinaryFieldSchema("test2"));
		Optional<JsonObject> op = provider.getConfig(schema);
		assertTrue(op.isPresent());
		assertEquals(1, op.get().getJsonArray("processors").size());

		// Two fields
		schema.addField(FieldUtil.createBinaryFieldSchema("test3"));
		Optional<JsonObject> op2 = provider.getConfig(schema);
		assertTrue(op2.isPresent());
		assertEquals(2, op2.get().getJsonArray("processors").size());
	}
}
