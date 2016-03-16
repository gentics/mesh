package com.gentics.mesh.core.field.microschema;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeGraphField;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.field.AbstractGraphFieldNodeVerticleTest;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModel;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.MicronodeField;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.BooleanFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.DateFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.HtmlFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NumberFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.util.FieldUtil;

public class MicronodeGraphFieldNodeVerticleTest extends AbstractGraphFieldNodeVerticleTest {
	protected final static String FIELDNAME = "micronodeField";

	@Before
	public void updateSchema() throws IOException {
		Schema schema = schemaContainer("folder").getLatestVersion().getSchema();
		MicronodeFieldSchema microschemaFieldSchema = new MicronodeFieldSchemaImpl();
		microschemaFieldSchema.setName(FIELDNAME);
		microschemaFieldSchema.setLabel("Some label");
		microschemaFieldSchema.setAllowedMicroSchemas(new String[] { "vcard" });
		schema.addField(microschemaFieldSchema);
		schemaContainer("folder").getLatestVersion().setSchema(schema);
	}

	@Test
	@Override
	public void testCreateNodeWithNoField() {
		NodeResponse response = createNodeAndCheck(FIELDNAME, (Field) null);
		MicronodeField field = response.getFields().getMicronodeField(FIELDNAME);
		assertNotNull(field);
		assertNull(field.getFields());
	}

	@Test
	@Override
	public void testUpdateNodeFieldWithField() {
		MicronodeResponse field = new MicronodeResponse();
		field.setMicroschema(new MicroschemaReference().setName("vcard"));
		field.getFields().put("firstName", new StringFieldImpl().setString("Max"));
		field.getFields().put("lastName", new StringFieldImpl().setString("Moritz"));
		NodeResponse response = updateNode(FIELDNAME, field);

		MicronodeResponse fieldResponse = response.getFields().getMicronodeField(FIELDNAME);
		String uuid = fieldResponse.getUuid();
		assertEquals("Check micronode firstName", "Max", fieldResponse.getFields().getStringField("firstName").getString());

		field = new MicronodeResponse();
		field.setMicroschema(new MicroschemaReference().setName("vcard"));
		field.getFields().put("firstName", new StringFieldImpl().setString("Moritz"));
		response = updateNode(FIELDNAME, field);

		fieldResponse = response.getFields().getMicronodeField(FIELDNAME);
		assertEquals("Check micronode firstName", "Moritz", fieldResponse.getFields().getStringField("firstName").getString());
		assertEquals("Check micronode uuid after update", uuid, fieldResponse.getUuid());
	}

	@Test
	@Override
	public void testCreateNodeWithField() {
		MicronodeResponse field = new MicronodeResponse();
		MicroschemaReference microschema = new MicroschemaReference();
		microschema.setName("vcard");
		field.setMicroschema(microschema);
		field.getFields().put("firstName", new StringFieldImpl().setString("Max"));
		field.getFields().put("lastName", new StringFieldImpl().setString("Mustermann"));
		NodeResponse response = createNodeAndCheck(FIELDNAME, field);

		MicronodeResponse createdField = response.getFields().getMicronodeField(FIELDNAME);
		assertNotNull("Created field does not exist", createdField);
		assertNotNull("Micronode has no uuid set", createdField.getUuid());

		assertEquals("Check microschema name", "vcard", createdField.getMicroschema().getName());
		assertEquals("Check microschema uuid", microschemaContainers().get("vcard").getUuid(), createdField.getMicroschema().getUuid());

		// check micronode fields
		StringField createdFirstnameField = createdField.getFields().getStringField("firstName");
		assertNotNull("Micronode did not contain firstName field", createdFirstnameField);
		assertEquals("Check micronode firstName", "Max", createdFirstnameField.getString());
	}

	@Test
	public void testCreateNodeWithInvalidMicroschema() {
		MicronodeResponse field = new MicronodeResponse();
		MicroschemaReference microschema = new MicroschemaReference();
		microschema.setName("notexisting");
		field.setMicroschema(microschema);
		field.getFields().put("firstName", new StringFieldImpl().setString("Max"));
		createNodeAndExpectFailure(FIELDNAME, field, BAD_REQUEST, "microschema_reference_invalid", "micronodeField");
	}

	@Test
	public void testCreateNodeWithNotAllowedMicroschema() {
		MicronodeResponse field = new MicronodeResponse();
		MicroschemaReference microschema = new MicroschemaReference();
		microschema.setName("captionedImage");
		field.setMicroschema(microschema);
		field.getFields().put("firstName", new StringFieldImpl().setString("Max"));
		createNodeAndExpectFailure(FIELDNAME, field, BAD_REQUEST, "node_error_invalid_microschema_field_value", "micronodeField", "captionedImage");
	}

	@Test
	@Override
	public void testReadNodeWithExistingField() throws IOException {
		MicroschemaContainerVersion microschema = microschemaContainers().get("vcard").getLatestVersion();
		Node node = folder("2015");

		NodeGraphFieldContainer container = node.getGraphFieldContainer(english());

		MicronodeGraphField micronodeField = container.createMicronode(FIELDNAME, microschema);
		micronodeField.getMicronode().createString("firstName").setString("Max");

		NodeResponse response = readNode(node);

		MicronodeResponse deserializedMicronodeField = response.getFields().getMicronodeField(FIELDNAME);
		assertNotNull("Micronode field must not be null", deserializedMicronodeField);
		StringField firstNameField = deserializedMicronodeField.getFields().getStringField("firstName");
		assertNotNull("Micronode must contain firstName field", firstNameField);
		assertEquals("Check firstName value", "Max", firstNameField.getString());
	}

	/**
	 * Test updating a node with a micronode containing all possible field types
	 * 
	 * @throws IOException
	 */
	@Test
	public void testUpdateFieldTypes() throws IOException {
		Long date = System.currentTimeMillis();
		Node newsOverview = content("news overview");
		Node newsFolder = folder("news");

		Microschema fullMicroschema = new MicroschemaModel();
		fullMicroschema.setName("full");

		// TODO implement BinaryField in Micronode
		//		fullMicroschema.addField(new BinaryFieldSchemaImpl().setName("binaryfield").setLabel("Binary Field"));
		fullMicroschema.addField(new BooleanFieldSchemaImpl().setName("booleanfield").setLabel("Boolean Field"));
		fullMicroschema.addField(new DateFieldSchemaImpl().setName("datefield").setLabel("Date Field"));
		fullMicroschema.addField(new HtmlFieldSchemaImpl().setName("htmlfield").setLabel("HTML Field"));
		// TODO implement BinaryField in Micronode
		//		fullMicroschema.addField(new ListFieldSchemaImpl().setListType("binary").setName("listfield-binary").setLabel("Binary List Field"));
		fullMicroschema.addField(new ListFieldSchemaImpl().setListType("boolean").setName("listfield-boolean").setLabel("Boolean List Field"));
		fullMicroschema.addField(new ListFieldSchemaImpl().setListType("date").setName("listfield-date").setLabel("Date List Field"));
		fullMicroschema.addField(new ListFieldSchemaImpl().setListType("html").setName("listfield-html").setLabel("Html List Field"));
		fullMicroschema.addField(new ListFieldSchemaImpl().setListType("node").setName("listfield-node").setLabel("Node List Field"));
		fullMicroschema.addField(new ListFieldSchemaImpl().setListType("number").setName("listfield-number").setLabel("Number List Field"));
		fullMicroschema.addField(new ListFieldSchemaImpl().setListType("string").setName("listfield-string").setLabel("String List Field"));
		fullMicroschema.addField(new NodeFieldSchemaImpl().setName("nodefield").setLabel("Node Field"));
		fullMicroschema.addField(new NumberFieldSchemaImpl().setName("numberfield").setLabel("Number Field"));
		fullMicroschema.addField(new StringFieldSchemaImpl().setName("stringfield").setLabel("String Field"));
		microschemaContainers().put("full", boot.microschemaContainerRoot().create(fullMicroschema, getRequestUser()));

		Schema schema = schemaContainer("folder").getLatestVersion().getSchema();
		MicronodeFieldSchema microschemaFieldSchema = new MicronodeFieldSchemaImpl();
		microschemaFieldSchema.setName("full");
		microschemaFieldSchema.setLabel("Micronode field");
		microschemaFieldSchema.setAllowedMicroSchemas(new String[] { "full" });
		schema.addField(microschemaFieldSchema);
		schemaContainer("folder").getLatestVersion().setSchema(schema);

		MicronodeResponse field = new MicronodeResponse();
		field.setMicroschema(new MicroschemaReference().setName("full"));
		field.getFields().put("booleanfield", FieldUtil.createBooleanField(true));
		field.getFields().put("datefield", FieldUtil.createDateField(date));
		field.getFields().put("htmlfield", FieldUtil.createHtmlField("<b>HTML</b> value"));
		field.getFields().put("listfield-boolean", FieldUtil.createBooleanListField(true, false));
		field.getFields().put("listfield-date", FieldUtil.createDateListField(date, 0L));
		field.getFields().put("listfield-html", FieldUtil.createHtmlListField("<b>first</b>", "<i>second</i>", "<u>third</u>"));
		field.getFields().put("listfield-node", FieldUtil.createNodeListField(newsOverview.getUuid(), newsFolder.getUuid()));
		field.getFields().put("listfield-number", FieldUtil.createNumberListField(47, 11));
		field.getFields().put("listfield-string", FieldUtil.createStringListField("first", "second", "third"));
		field.getFields().put("nodefield", FieldUtil.createNodeField(newsOverview.getUuid()));
		field.getFields().put("numberfield", FieldUtil.createNumberField(4711));
		field.getFields().put("stringfield", FieldUtil.createStringField("String value"));

		NodeResponse response = updateNode("full", field);
		assertThat(response.getFields().getMicronodeField("full")).matches(field, fullMicroschema);
	}
}
