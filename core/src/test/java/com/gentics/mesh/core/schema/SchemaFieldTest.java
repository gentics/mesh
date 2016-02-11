package com.gentics.mesh.core.schema;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.core.field.bool.AbstractBasicDBTest;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.BooleanFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.DateFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.HtmlFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NumberFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.json.MeshJsonException;
import com.gentics.mesh.util.FieldUtil;

public class SchemaFieldTest extends AbstractBasicDBTest {

	@Test
	public void testSimpleSchema() throws IOException {
		Schema schema = new SchemaImpl();
		schema.setName("dummySchema");
		schema.setContainer(true);
		schema.addField(new HtmlFieldSchemaImpl().setLabel("Label").setName("Name").setRequired(true));
		validateSchema(schema);
	}

	@Test
	public void testComplexSchema() throws IOException {
		Schema schema = new SchemaImpl();
		schema.setName("dummySchema");
		schema.setDisplayField("name");
		schema.setSegmentField("name");
		schema.setContainer(true);
		schema.addField(FieldUtil.createStringFieldSchema("name"));
		schema.addField(FieldUtil.createHtmlFieldSchema("name_1").setLabel("label_1").setRequired(true));
		schema.addField(FieldUtil.createStringFieldSchema("name_2").setLabel("label_2").setRequired(true));
		schema.addField(FieldUtil.createNumberFieldSchema("name_3").setLabel("label_3").setRequired(true));
		schema.addField(FieldUtil.createDateFieldSchema("name_4").setLabel("label_4").setRequired(true));
		schema.addField(FieldUtil.createBooleanFieldSchema("name_5").setLabel("label_5").setRequired(true));

		ListFieldSchema listFieldSchema = new ListFieldSchemaImpl();
		listFieldSchema.setLabel("label_7").setName("name_7").setRequired(true);
		listFieldSchema.setAllowedSchemas(new String[] { "folder", "videos" });
		listFieldSchema.setListType("node");
		listFieldSchema.setMax(10);
		listFieldSchema.setMin(3);
		schema.addField(listFieldSchema);

		// MicroschemaFieldSchema microschemaFieldSchema = new MicroschemaFieldSchemaImpl();
		// microschemaFieldSchema.setLabel("label_8").setName("name_8").setRequired(true);
		// microschemaFieldSchema.setAllowedMicroSchemas(new String[] { "content", "folder" });
		//
		// StringFieldSchema stringFieldSchema = new StringFieldSchemaImpl();
		// stringFieldSchema.setName("field1").setLabel("label1");
		// microschemaFieldSchema.getFields().add(stringFieldSchema);
		// schema.addField(microschemaFieldSchema);

		schema.validate();
		validateSchema(schema);
	}

	@Test(expected = MeshJsonException.class)
	public void testConflictingFieldName() throws MeshJsonException {
		Schema schema = new SchemaImpl();
		schema.setName("dummySchema");
		schema.setContainer(true);
		schema.addField(new HtmlFieldSchemaImpl().setLabel("Label1").setName("name").setRequired(true));
		schema.addField(new HtmlFieldSchemaImpl().setLabel("Label2").setName("name").setRequired(true));
		schema.validate();
	}

	@Test(expected = MeshJsonException.class)
	public void testConflictingFieldLabel() throws MeshJsonException {
		Schema schema = new SchemaImpl();
		schema.setName("dummySchema");
		schema.setContainer(true);
		schema.addField(new HtmlFieldSchemaImpl().setLabel("Label").setName("name1").setRequired(true));
		schema.addField(new HtmlFieldSchemaImpl().setLabel("Label").setName("name2").setRequired(true));
		schema.validate();
	}

	private void validateSchema(Schema schema) throws JsonParseException, JsonMappingException, IOException {
		assertNotNull(schema);
		String json = JsonUtil.toJson(schema);
		System.out.println(json);
		assertNotNull(json);
		Schema deserializedSchema = JsonUtil.readSchema(json, SchemaImpl.class);
		assertEquals(schema.getFields().size(), deserializedSchema.getFields().size());
		assertNotNull(deserializedSchema);
	}

}
