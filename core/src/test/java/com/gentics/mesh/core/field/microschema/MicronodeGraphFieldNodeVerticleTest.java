package com.gentics.mesh.core.field.microschema;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.data.MicroschemaContainer;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeGraphField;
import com.gentics.mesh.core.field.AbstractGraphFieldNodeVerticleTest;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.MicronodeField;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;

public class MicronodeGraphFieldNodeVerticleTest extends AbstractGraphFieldNodeVerticleTest {
	protected final static String FIELDNAME = "micronodeField";

	@Before
	public void updateSchema() throws IOException {
		Schema schema = schemaContainer("folder").getSchema();
		MicronodeFieldSchema microschemaFieldSchema = new MicronodeFieldSchemaImpl();
		microschemaFieldSchema.setName(FIELDNAME);
		microschemaFieldSchema.setLabel("Some label");
		microschemaFieldSchema.setAllowedMicroSchemas(new String [] {"vcard"});
		schema.addField(microschemaFieldSchema);
		schemaContainer("folder").setSchema(schema);
	}

	@Test
	@Override
	public void testCreateNodeWithNoField() {
		NodeResponse response = createNode(FIELDNAME, (Field) null);
		MicronodeField field = response.getField(FIELDNAME);
		assertNotNull(field);
		assertNull(field.getFields());
	}

	@Test
	@Override
	public void testUpdateNodeFieldWithField() {
		MicronodeResponse field = new MicronodeResponse();
		field.setMicroschema(new MicroschemaReference().setName("vcard"));
		field.getFields().put("firstName", new StringFieldImpl().setString("Max"));
		NodeResponse response = updateNode(FIELDNAME, field);

		MicronodeResponse fieldResponse = response.getField(FIELDNAME);
		String uuid = fieldResponse.getUuid();
		assertEquals("Check micronode firstName", "Max", fieldResponse.getField("firstName", StringField.class).getString());

		field = new MicronodeResponse();
		field.setMicroschema(new MicroschemaReference().setName("vcard"));
		field.getFields().put("firstName", new StringFieldImpl().setString("Moritz"));
		response = updateNode(FIELDNAME, field);

		fieldResponse = response.getField(FIELDNAME);
		assertEquals("Check micronode firstName", "Moritz", fieldResponse.getField("firstName", StringField.class).getString());
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
		NodeResponse response = createNode(FIELDNAME, field);

		MicronodeResponse createdField = response.getField(FIELDNAME);
		assertNotNull("Created field does not exist", createdField);
		assertNotNull("Micronode has no uuid set", createdField.getUuid());

		assertEquals("Check microschema name", "vcard", createdField.getMicroschema().getName());
		assertEquals("Check microschema uuid", microschemaContainers().get("vcard").getUuid(), createdField.getMicroschema().getUuid());

		// check micronode fields
		StringField createdFirstnameField = createdField.getField("firstName");
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
		// TODO assert translated error message
		createNodeFailure(FIELDNAME, field, BAD_REQUEST, "Could not parse request JSON.");
	}

	@Test
	public void testCreateNodeWithNotAllowedMicroschema() {
		MicronodeResponse field = new MicronodeResponse();
		MicroschemaReference microschema = new MicroschemaReference();
		microschema.setName("captionedImage");
		field.setMicroschema(microschema);
		field.getFields().put("firstName", new StringFieldImpl().setString("Max"));
		// TODO assert translated error message
		createNodeFailure(FIELDNAME, field, BAD_REQUEST, "Could not parse request JSON.");
	}

	@Test
	@Override
	public void testReadNodeWithExistingField() throws IOException {
		MicroschemaContainer microschema = microschemaContainers().get("vcard");
		Node node = folder("2015");

		NodeGraphFieldContainer container = node.getGraphFieldContainer(english());
		
		MicronodeGraphField micronodeField = container.createMicronode(FIELDNAME, microschema);
		micronodeField.getMicronode().createString("firstName").setString("Max");

		NodeResponse response = readNode(node);

		MicronodeResponse deserializedMicronodeField = response.getField(FIELDNAME, MicronodeResponse.class);
		assertNotNull("Micronode field must not be null", deserializedMicronodeField);
		StringField firstNameField = deserializedMicronodeField.getField("firstName");
		assertNotNull("Micronode must contain firstName field", firstNameField);
		assertEquals("Check firstName value", "Max", firstNameField.getString());
	}

}
