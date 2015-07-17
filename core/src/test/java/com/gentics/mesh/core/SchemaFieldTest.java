package com.gentics.mesh.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.MicroschemaFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SelectFieldSchema;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.BooleanFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.DateFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.HTMLFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicroschemaFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NumberFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SelectFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.json.MeshJsonException;
import com.gentics.mesh.test.AbstractDBTest;

public class SchemaFieldTest extends AbstractDBTest {

	@Autowired
	private ServerSchemaStorage schemaStorage;

	@Before
	public void setup() throws Exception {
		setupData();
	}

	@Test
	public void testSimpleSchema() throws IOException {
		Schema schema = new SchemaImpl();
		schema.setName("dummySchema");
		schema.setBinary(true);
		schema.setFolder(true);
		schema.addField(new HTMLFieldSchemaImpl().setLabel("Label").setName("Name").setRequired(true));
		validateSchema(schema);
	}

	@Test
	public void testComplexSchema() throws IOException {
		Schema schema = new SchemaImpl();
		schema.setName("dummySchema");
		schema.setBinary(true);
		schema.setFolder(true);
		schema.addField(new HTMLFieldSchemaImpl().setLabel("label_1").setName("name_1").setRequired(true));
		schema.addField(new StringFieldSchemaImpl().setLabel("label_2").setName("name_2").setRequired(true));
		schema.addField(new NumberFieldSchemaImpl().setLabel("label_3").setName("name_3").setRequired(true));
		schema.addField(new DateFieldSchemaImpl().setLabel("label_4").setName("name_4").setRequired(true));
		schema.addField(new BooleanFieldSchemaImpl().setLabel("label_5").setName("name_5").setRequired(true));

		SelectFieldSchema selectFieldSchema = new SelectFieldSchemaImpl();
		selectFieldSchema.setLabel("label_6").setName("name_6").setRequired(true);
		List<String> options = new ArrayList<>();
		options.add("option_1");
		options.add("option_2");
		options.add("option_3");
		selectFieldSchema.setSelections(options);
		schema.addField(selectFieldSchema);

		ListFieldSchema listFieldSchema = new ListFieldSchemaImpl();
		listFieldSchema.setLabel("label_7").setName("name_7").setRequired(true);
		listFieldSchema.setAllowedSchemas(new String[] { "folder", "videos" });
		listFieldSchema.setListType("node");
		listFieldSchema.setMax(10);
		listFieldSchema.setMin(3);
		schema.addField(listFieldSchema);

//		MicroschemaFieldSchema microschemaFieldSchema = new MicroschemaFieldSchemaImpl();
//		microschemaFieldSchema.setLabel("label_8").setName("name_8").setRequired(true);
//		microschemaFieldSchema.setAllowedMicroSchemas(new String[] { "content", "folder" });
//		
//		StringFieldSchema stringFieldSchema = new StringFieldSchemaImpl();
//		stringFieldSchema.setName("field1").setLabel("label1");
//		microschemaFieldSchema.getFields().add(stringFieldSchema);
//		schema.addField(microschemaFieldSchema);

		schema.validate();
		validateSchema(schema);
	}

	@Test(expected = MeshJsonException.class)
	public void testConflictingFieldName() throws MeshJsonException {
		Schema schema = new SchemaImpl();
		schema.setName("dummySchema");
		schema.setBinary(true);
		schema.setFolder(true);
		schema.addField(new HTMLFieldSchemaImpl().setLabel("Label1").setName("name").setRequired(true));
		schema.addField(new HTMLFieldSchemaImpl().setLabel("Label2").setName("name").setRequired(true));
		schema.validate();
	}

	@Test(expected = MeshJsonException.class)
	public void testConflictingFieldLabel() throws MeshJsonException {
		Schema schema = new SchemaImpl();
		schema.setName("dummySchema");
		schema.setBinary(true);
		schema.setFolder(true);
		schema.addField(new HTMLFieldSchemaImpl().setLabel("Label").setName("name1").setRequired(true));
		schema.addField(new HTMLFieldSchemaImpl().setLabel("Label").setName("name2").setRequired(true));
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
