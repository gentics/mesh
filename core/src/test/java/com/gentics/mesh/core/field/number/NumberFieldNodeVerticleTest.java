package com.gentics.mesh.core.field.number;

import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.NumberGraphField;
import com.gentics.mesh.core.field.AbstractFieldNodeVerticleTest;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.NumberFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.impl.NumberFieldSchemaImpl;
import com.gentics.mesh.parameter.impl.NodeParameters;

import io.vertx.core.Future;

public class NumberFieldNodeVerticleTest extends AbstractFieldNodeVerticleTest {
	private static final String FIELD_NAME = "numberField";

	@Before
	public void updateSchema() throws IOException {
		Schema schema = schemaContainer("folder").getLatestVersion().getSchema();
		NumberFieldSchema numberFieldSchema = new NumberFieldSchemaImpl();
		numberFieldSchema.setName(FIELD_NAME);
		// numberFieldSchema.setMin(10);
		// numberFieldSchema.setMax(1000);
		schema.addField(numberFieldSchema);
		schemaContainer("folder").getLatestVersion().setSchema(schema);
	}

	@Test
	@Override
	public void testCreateNodeWithNoField() {
		NodeResponse response = createNode(FIELD_NAME, (Field) null);
		NumberFieldImpl field = response.getFields().getNumberField(FIELD_NAME);
		assertNull("The field should be null since we did not specify a field when executing the creation call", field);
	}

	@Test
	public void testCreateNodeWithWrongFieldType() {
		String fieldKey = FIELD_NAME;
		StringField field = new StringFieldImpl().setString("text");

		Node node = folder("2015");
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setParentNodeUuid(node.getUuid());
		nodeCreateRequest.setSchema(new SchemaReference().setName("folder"));
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.getFields().put(fieldKey, field);

		Future<NodeResponse> future = getClient().createNode(PROJECT_NAME, nodeCreateRequest, new NodeParameters().setLanguages("en"));
		latchFor(future);
		expectException(future, BAD_REQUEST, "field_number_error_invalid_type", fieldKey, "text");
	}

	@Test
	@Override
	public void testUpdateNodeFieldWithField() {
		Node node = folder("2015");
		for (int i = 0; i < 20; i++) {
			NodeGraphFieldContainer container = node.getGraphFieldContainer("en");
			Number oldValue = getNumberValue(container, FIELD_NAME);
			Number newValue = Integer.valueOf(i + 42);

			NodeResponse response = updateNode(FIELD_NAME, new NumberFieldImpl().setNumber(newValue));
			NumberFieldImpl field = response.getFields().getNumberField(FIELD_NAME);
			assertEquals(newValue, field.getNumber());
			node.reload();
			container.reload();

			assertEquals("Check version number", container.getVersion().nextDraft().toString(), response.getVersion().getNumber());
			assertEquals("Check old value", oldValue, getNumberValue(container, FIELD_NAME));
		}
	}

	@Test
	@Override
	public void testUpdateSameValue() {
		NodeResponse firstResponse = updateNode(FIELD_NAME, new NumberFieldImpl().setNumber(42));
		String oldNumber = firstResponse.getVersion().getNumber();

		NodeResponse secondResponse = updateNode(FIELD_NAME, new NumberFieldImpl().setNumber(42));
		assertThat(secondResponse.getVersion().getNumber()).as("New version number").isEqualTo(oldNumber);
	}

	@Test
	@Override
	public void testUpdateSetNull() {
		NodeResponse response = updateNode(FIELD_NAME, new NumberFieldImpl().setNumber(42));

		// Field should be deleted
		response = updateNode(FIELD_NAME, null);
		assertThat(response.getFields().getNumberField(FIELD_NAME)).as("Updated Field").isNull();

		// Update again to restore a value
		updateNode(FIELD_NAME, new NumberFieldImpl().setNumber(42));

	}

	@Test
	@Override
	public void testUpdateSetEmpty() {
		// Number fields can't be set to empty - The rest model will generate a null field for the update request json. Thus the field will be deleted.
		NodeResponse response = updateNode(FIELD_NAME, new NumberFieldImpl());
		assertThat(response.getFields().getNumberField(FIELD_NAME)).as("Updated Field").isNull();
	}

	/**
	 * Get the number value
	 * 
	 * @param container
	 *            container
	 * @param fieldName
	 *            field name
	 * @return number value (may be null)
	 */
	protected Number getNumberValue(NodeGraphFieldContainer container, String fieldName) {
		NumberGraphField field = container.getNumber(fieldName);
		return field != null ? field.getNumber() : null;
	}

	@Test
	@Override
	public void testCreateNodeWithField() {
		NodeResponse response = createNode(FIELD_NAME, new NumberFieldImpl().setNumber(1.21));
		NumberFieldImpl numberField = response.getFields().getNumberField(FIELD_NAME);
		assertEquals(1.21, numberField.getNumber());
	}

	@Test
	@Override
	public void testReadNodeWithExistingField() throws IOException {
		Node node = folder("2015");

		NodeGraphFieldContainer container = node.getLatestDraftFieldContainer(english());
		NumberGraphField numberField = container.createNumber(FIELD_NAME);
		numberField.setNumber(100.9f);

		NodeResponse response = readNode(node);

		NumberFieldImpl deserializedNumberField = response.getFields().getNumberField(FIELD_NAME);
		assertNotNull(deserializedNumberField);
		assertEquals(100.9, deserializedNumberField.getNumber());
	}

}
