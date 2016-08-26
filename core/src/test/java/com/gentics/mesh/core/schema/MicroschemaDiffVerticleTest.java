package com.gentics.mesh.core.schema;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.ADDFIELD;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.REMOVEFIELD;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.UPDATEMICROSCHEMA;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.AbstractIsolatedRestVerticleTest;

import io.vertx.core.AbstractVerticle;

public class MicroschemaDiffVerticleTest extends AbstractIsolatedRestVerticleTest {

	@Override
	public List<AbstractVerticle> getAdditionalVertices() {
		List<AbstractVerticle> list = new ArrayList<>();
		list.add(meshDagger.microschemaVerticle());
		return list;
	}

	private Microschema getMicroschema() {
		Microschema vcardMicroschema = new MicroschemaModel();
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
		try (NoTx noTx = db.noTx()) {
			MicroschemaContainer microschema = microschemaContainer("vcard");
			Microschema request = getMicroschema();
			MeshResponse<SchemaChangesListModel> future = getClient().diffMicroschema(microschema.getUuid(), request).invoke();
			latchFor(future);
			assertSuccess(future);
			SchemaChangesListModel changes = future.result();
			assertNotNull(changes);
			assertThat(changes.getChanges()).isEmpty();
		}
	}

	@Test
	public void testAddField() {
		try (NoTx noTx = db.noTx()) {
			MicroschemaContainer microschema = microschemaContainer("vcard");
			Microschema request = getMicroschema();
			StringFieldSchema stringField = FieldUtil.createStringFieldSchema("someField");
			stringField.setAllowedValues("one", "two");
			request.addField(stringField);

			MeshResponse<SchemaChangesListModel> future = getClient().diffMicroschema(microschema.getUuid(), request).invoke();
			latchFor(future);
			assertSuccess(future);
			SchemaChangesListModel changes = future.result();
			assertNotNull(changes);
			assertThat(changes.getChanges()).hasSize(2);
			assertThat(changes.getChanges().get(0)).is(ADDFIELD).forField("someField");
			assertThat(changes.getChanges().get(1)).is(UPDATEMICROSCHEMA).hasProperty("order",
					new String[] { "firstName", "lastName", "address", "postcode", "someField" });
		}
	}

	@Test
	public void testAddUnsupportedField() {
		try (NoTx noTx = db.noTx()) {
			MicroschemaContainer microschema = microschemaContainer("vcard");
			Microschema request = getMicroschema();
			BinaryFieldSchema binaryField = FieldUtil.createBinaryFieldSchema("binaryField");
			request.addField(binaryField);

			MeshResponse<SchemaChangesListModel> future = getClient().diffMicroschema(microschema.getUuid(), request).invoke();
			latchFor(future);
			expectException(future, BAD_REQUEST, "microschema_error_field_type_not_allowed", "binaryField", "binary");
		}
	}

	@Test
	public void testRemoveField() {
		try (NoTx noTx = db.noTx()) {
			MicroschemaContainer microschema = microschemaContainer("vcard");
			Microschema request = getMicroschema();
			request.removeField("postcode");
			MeshResponse<SchemaChangesListModel> future = getClient().diffMicroschema(microschema.getUuid(), request).invoke();
			latchFor(future);
			assertSuccess(future);
			SchemaChangesListModel changes = future.result();
			assertNotNull(changes);
			assertThat(changes.getChanges()).hasSize(2);
			assertThat(changes.getChanges().get(0)).is(REMOVEFIELD).forField("postcode");
			assertThat(changes.getChanges().get(1)).is(UPDATEMICROSCHEMA).hasProperty("order", new String[] { "firstName", "lastName", "address" });
		}
	}

}