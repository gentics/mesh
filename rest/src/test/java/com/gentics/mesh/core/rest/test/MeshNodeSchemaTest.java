package com.gentics.mesh.core.rest.test;

import org.junit.Test;

import com.gentics.mesh.core.rest.node.field.NodeField;
import com.gentics.mesh.core.rest.schema.BooleanFieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.BooleanFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.request.SchemaCreateRequest;

public class MeshNodeSchemaTest {

	@Test
	public void testNodeSchemaCreateRequest() {
		Schema schema = new SchemaImpl();

		schema.setName("blogpost");
		schema.setDisplayField("name");

//		assertEquals("bogusUUID", request.getUuid());
//		assertEquals("name", request.getDisplayField());
//		assertEquals("blogpost", request.getName());

		StringFieldSchema stringSchema = new StringFieldSchemaImpl();
		stringSchema.setLabel("string field label");
		stringSchema.setName("string field name");
		schema.addField(stringSchema);

		BooleanFieldSchema booleanSchema = new BooleanFieldSchemaImpl();
		booleanSchema.setLabel("boolean field label");
		booleanSchema.setName("boolean field name");
		booleanSchema.setValue(true);
		schema.addField(booleanSchema);

		ListFieldSchema<NodeField> listFieldSchema = new ListFieldSchemaImpl<>();
		listFieldSchema.setName("list field name");
		listFieldSchema.setLabel("list field label");
		listFieldSchema.setListType("node");
		listFieldSchema.setMin(5);
		listFieldSchema.setMax(10);
		listFieldSchema.setAllowedSchemas(new String[] { "image", "gallery" });


		
		for (FieldSchema field : schema.getFields()) {
			System.out.println(field.getName());
			System.out.println(field.getLabel());
			System.out.println(field.getType());
			System.out.println();
		}
		
		SchemaCreateRequest request = new SchemaCreateRequest();
		request.setSchema(schema);

	}
}
