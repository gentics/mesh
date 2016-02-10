package com.gentics.mesh.core.schema;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.ADDFIELD;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.REMOVEFIELD;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.MicroschemaContainer;
import com.gentics.mesh.core.rest.schema.HtmlFieldSchema;
import com.gentics.mesh.core.rest.schema.MicroschemaUpdateRequest;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.rest.schema.impl.HtmlFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.core.verticle.microschema.MicroschemaVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;
import com.gentics.mesh.util.FieldUtil;

import io.vertx.core.Future;

public class MicroschemaDiffVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private MicroschemaVerticle verticle;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(verticle);
		return list;
	}

	private MicroschemaUpdateRequest getMicroschemaUpdateRequest() {
		MicroschemaUpdateRequest request = new MicroschemaUpdateRequest();
		request.setName("content");

		StringFieldSchema nameFieldSchema = new StringFieldSchemaImpl();
		nameFieldSchema.setName("name");
		nameFieldSchema.setLabel("Name");
		nameFieldSchema.setRequired(true);
		request.addField(nameFieldSchema);

		StringFieldSchema filenameFieldSchema = new StringFieldSchemaImpl();
		filenameFieldSchema.setName("filename");
		filenameFieldSchema.setLabel("Filename");
		request.addField(filenameFieldSchema);

		StringFieldSchema titleFieldSchema = new StringFieldSchemaImpl();
		titleFieldSchema.setName("title");
		titleFieldSchema.setLabel("Title");
		request.addField(titleFieldSchema);

		HtmlFieldSchema contentFieldSchema = new HtmlFieldSchemaImpl();
		contentFieldSchema.setName("content");
		contentFieldSchema.setLabel("Content");
		request.addField(contentFieldSchema);
		return request;
	}

	@Test
	public void testNoDiff() {
		MicroschemaContainer microschema = microschemaContainer("vcard");
		MicroschemaUpdateRequest request = getMicroschemaUpdateRequest();
		Future<SchemaChangesListModel> future = getClient().diffMicroschema(microschema.getUuid(), request);
		latchFor(future);
		assertSuccess(future);
		SchemaChangesListModel changes = future.result();
		assertNotNull(changes);
		assertThat(changes.getChanges()).isEmpty();
	}

	@Test
	public void testAddField() {
		MicroschemaContainer microschema = microschemaContainer("vcard");
		MicroschemaUpdateRequest request = getMicroschemaUpdateRequest();
		StringFieldSchema stringField = FieldUtil.createStringFieldSchema("someField");
		stringField.setAllowedValues("one", "two");
		request.addField(stringField);

		Future<SchemaChangesListModel> future = getClient().diffMicroschema(microschema.getUuid(), request);
		latchFor(future);
		assertSuccess(future);
		SchemaChangesListModel changes = future.result();
		assertNotNull(changes);
		assertThat(changes.getChanges()).hasSize(1);
		assertThat(changes.getChanges().get(0)).is(ADDFIELD).forField("someField");
	}

	@Test
	public void testAddUnsupportedField() {
		fail("Test adding a binary field. Micronodes can't have binary fields. Expect an error");
	}

	@Test
	public void testRemoveField() {
		MicroschemaContainer microschema = microschemaContainer("vcard");
		MicroschemaUpdateRequest request = getMicroschemaUpdateRequest();
		request.removeField("content");
		Future<SchemaChangesListModel> future = getClient().diffMicroschema(microschema.getUuid(), request);
		latchFor(future);
		assertSuccess(future);
		SchemaChangesListModel changes = future.result();
		assertNotNull(changes);
		assertThat(changes.getChanges()).hasSize(1);
		assertThat(changes.getChanges().get(0)).is(REMOVEFIELD).forField("content");
	}

}
