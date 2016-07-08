package com.gentics.mesh.core.field.binary;

import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.field.AbstractFieldNodeVerticleTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.impl.BinaryFieldImpl;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;

import io.vertx.core.buffer.Buffer;
import io.vertx.test.core.TestUtils;

public class BinaryFieldVerticleTest extends AbstractFieldNodeVerticleTest {

	private static final String FIELD_NAME = "binaryField";

	/**
	 * Update the schema and add a binary field.
	 * 
	 * @throws IOException
	 */
	@Before
	public void updateSchema() throws IOException {
		setSchema(false);
	}

	private void setSchema(boolean isRequired) {
		Schema schema = schemaContainer("folder").getLatestVersion().getSchema();

		// add non restricted string field
		BinaryFieldSchema binaryFieldSchema = new BinaryFieldSchemaImpl();
		binaryFieldSchema.setName(FIELD_NAME);
		binaryFieldSchema.setLabel("Some label");
		binaryFieldSchema.setRequired(isRequired);
		schema.addField(binaryFieldSchema);

		schemaContainer("folder").getLatestVersion().setSchema(schema);

	}

	@Override
	public void testReadNodeWithExistingField() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void testUpdateNodeFieldWithField() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testUpdateSameValue() {
		//1. Upload a binary field
		String uuid = db.noTrx(() -> folder("2015").getUuid());
		Buffer buffer = TestUtils.randomBuffer(1000);
		call(() -> getClient().updateNodeBinaryField(PROJECT_NAME, uuid, "en", FIELD_NAME, buffer, "filename.txt", "application/binary"));

		NodeResponse firstResponse = call(() -> getClient().findNodeByUuid(PROJECT_NAME, uuid));
		assertEquals("filename.txt", firstResponse.getFields().getBinaryField(FIELD_NAME).getFileName());
		String oldVersion = firstResponse.getVersion().getNumber();

		BinaryField binaryField = firstResponse.getFields().getBinaryField(FIELD_NAME);

		// 2. Update the node using the loaded binary field data 
		NodeResponse secondResponse = updateNode(FIELD_NAME, binaryField);
		assertThat(secondResponse.getFields().getBinaryField(FIELD_NAME)).as("Updated Field").isNotNull();
		assertThat(secondResponse.getVersion().getNumber()).as("New version number").isNotEqualTo(oldVersion);
	}

	@Test
	@Override
	public void testUpdateSetNull() {

		//1. Upload a binary field
		String uuid = db.noTrx(() -> folder("2015").getUuid());
		Buffer buffer = TestUtils.randomBuffer(1000);
		call(() -> getClient().updateNodeBinaryField(PROJECT_NAME, uuid, "en", FIELD_NAME, buffer, "filename.txt", "application/binary"));

		NodeResponse firstResponse = call(() -> getClient().findNodeByUuid(PROJECT_NAME, uuid));
		String oldVersion = firstResponse.getVersion().getNumber();

		// 2. Set the field to null
		NodeResponse secondResponse = updateNode(FIELD_NAME, null);
		assertThat(secondResponse.getFields().getBinaryField(FIELD_NAME)).as("Updated Field").isNull();
		assertThat(secondResponse.getVersion().getNumber()).as("New version number").isNotEqualTo(oldVersion);

		// 3. Set the field to null one more time and assert that no new version was created
		NodeResponse thirdResponse = updateNode(FIELD_NAME, null);
		assertEquals("The field does not change and thus the version should not be bumped.", thirdResponse.getVersion().getNumber(),
				secondResponse.getVersion().getNumber());

	}

	@Test
	@Override
	public void testUpdateSetEmpty() {
		//1. Upload a binary field
		String uuid = db.noTrx(() -> folder("2015").getUuid());
		Buffer buffer = TestUtils.randomBuffer(1000);
		call(() -> getClient().updateNodeBinaryField(PROJECT_NAME, uuid, "en", FIELD_NAME, buffer, "filename.txt", "application/binary"));

		NodeResponse firstResponse = call(() -> getClient().findNodeByUuid(PROJECT_NAME, uuid));
		assertEquals("filename.txt", firstResponse.getFields().getBinaryField(FIELD_NAME).getFileName());
		String oldVersion = firstResponse.getVersion().getNumber();

		// 2. Set the field to empty
		NodeResponse secondResponse = updateNode(FIELD_NAME, new BinaryFieldImpl());
		assertThat(secondResponse.getFields().getBinaryField(FIELD_NAME)).as("Updated Field").isNotNull();
		assertThat(secondResponse.getVersion().getNumber()).as("New version number").isNotEqualTo(oldVersion);

		fail("Decide what should happen");
	}

	@Override
	public void testCreateNodeWithField() {
		// TODO Auto-generated method stub

	}

	@Override
	public void testCreateNodeWithNoField() {
		// TODO Auto-generated method stub

	}

}
