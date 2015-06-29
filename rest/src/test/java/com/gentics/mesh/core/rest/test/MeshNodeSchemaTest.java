package com.gentics.mesh.core.rest.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.mesh.core.rest.schema.BooleanFieldSchema;
import com.gentics.mesh.core.rest.schema.BooleanFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.StringFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.request.SchemaCreateRequest;

public class MeshNodeSchemaTest {

	@Test
	public void testNodeSchemaCreateRequest() {
		SchemaCreateRequest request = new SchemaCreateRequest();
		request.setName("blogpost");
		request.setDisplayField("name");
		request.setUuid("bogusUUID");

		assertEquals("bogusUUID", request.getUuid());
		assertEquals("name", request.getDisplayField());
		assertEquals("blogpost", request.getName());

		StringFieldSchema stringSchema = new StringFieldSchemaImpl();
		stringSchema.setLabel("string field label");
		stringSchema.setName("string field name");
		request.addField(stringSchema);

		BooleanFieldSchema booleanSchema = new BooleanFieldSchemaImpl();
		booleanSchema.setLabel("boolean field label");
		booleanSchema.setName("boolean field name");
		booleanSchema.setValue(true);
		request.addField(booleanSchema);

		for (FieldSchema field : request.getFields()) {
			System.out.println(field.getName());
			System.out.println(field.getLabel());
			System.out.println(field.getType());
			System.out.println();
		}
	}
}
