package com.gentics.mesh.core.schema;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.syncleus.ferma.tx.Tx;
import org.junit.Test;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.ADDFIELD;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.REMOVEFIELD;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.UPDATEMICROSCHEMA;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.junit.Assert.assertNotNull;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class MicroschemaDiffEndpointTest extends AbstractMeshTest {

	private Microschema getMicroschema() {
		Microschema vcardMicroschema = new MicroschemaModelImpl();
		vcardMicroschema.setName("vcard");
		vcardMicroschema.setDescription("Microschema for a vcard");

		// firstname field
		StringFieldSchema firstNameFieldSchema = new StringFieldSchemaImpl();
		firstNameFieldSchema.setName("firstName");
		firstNameFieldSchema.setLabel("First Name");
		firstNameFieldSchema.setRequired(true);
		vcardMicroschema.addField(firstNameFieldSchema);

		// lastname field
		StringFieldSchema lastNameFieldSchema = new StringFieldSchemaImpl();
		lastNameFieldSchema.setName("lastName");
		lastNameFieldSchema.setLabel("Last Name");
		lastNameFieldSchema.setRequired(true);
		vcardMicroschema.addField(lastNameFieldSchema);

		// address field
		StringFieldSchema addressFieldSchema = new StringFieldSchemaImpl();
		addressFieldSchema.setName("address");
		addressFieldSchema.setLabel("Address");
		vcardMicroschema.addField(addressFieldSchema);

		// postcode field
		StringFieldSchema postcodeFieldSchema = new StringFieldSchemaImpl();
		postcodeFieldSchema.setName("postcode");
		postcodeFieldSchema.setLabel("Post Code");
		vcardMicroschema.addField(postcodeFieldSchema);

		return vcardMicroschema;
	}

	@Test
	public void testNoDiff() {
		try (Tx tx = tx()) {
			MicroschemaContainer microschema = microschemaContainer("vcard");
			Microschema request = getMicroschema();

			SchemaChangesListModel changes = call(() -> client().diffMicroschema(microschema.getUuid(), request));
			assertNotNull(changes);
			assertThat(changes.getChanges()).isEmpty();
		}
	}

	@Test
	public void testAddField() {
		try (Tx tx = tx()) {
			MicroschemaContainer microschema = microschemaContainer("vcard");
			Microschema request = getMicroschema();
			StringFieldSchema stringField = FieldUtil.createStringFieldSchema("someField");
			stringField.setAllowedValues("one", "two");
			request.addField(stringField);
			SchemaChangesListModel changes = call(() ->

			client().diffMicroschema(microschema.getUuid(), request));
			assertNotNull(changes);
			assertThat(changes.getChanges()).hasSize(2);
			assertThat(changes.getChanges().get(0)).is(ADDFIELD).forField("someField");
			assertThat(changes.getChanges().get(1)).is(UPDATEMICROSCHEMA).hasProperty("order",
					new String[] { "firstName", "lastName", "address", "postcode", "someField" });
			call(() -> client().applyChangesToMicroschema(microschema.getUuid(), changes));
		}
	}

	@Test
	public void testAddUnsupportedField() {
		try (Tx tx = tx()) {
			MicroschemaContainer microschema = microschemaContainer("vcard");
			Microschema request = getMicroschema();
			BinaryFieldSchema binaryField = FieldUtil.createBinaryFieldSchema("binaryField");
			request.addField(binaryField);

			call(() -> client().diffMicroschema(microschema.getUuid(), request), BAD_REQUEST, "microschema_error_field_type_not_allowed",
					"binaryField", "binary");
		}
	}

	@Test
	public void testRemoveField() {
		try (Tx tx = tx()) {
			MicroschemaContainer microschema = microschemaContainer("vcard");
			Microschema request = getMicroschema();
			request.removeField("postcode");

			SchemaChangesListModel changes = call(() -> client().diffMicroschema(microschema.getUuid(), request));
			assertNotNull(changes);
			assertThat(changes.getChanges()).hasSize(2);
			assertThat(changes.getChanges().get(0)).is(REMOVEFIELD).forField("postcode");
			assertThat(changes.getChanges().get(1)).is(UPDATEMICROSCHEMA).hasProperty("order", new String[] { "firstName", "lastName", "address" });
		}
	}

}