package com.gentics.mesh.core.field.number;

import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.NumberGraphField;
import com.gentics.mesh.core.field.AbstractGraphFieldNodeVerticleTest;
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
import com.gentics.mesh.query.impl.NodeRequestParameter;

import io.vertx.core.Future;

public class NumberGraphFieldNodeVerticleTest extends AbstractGraphFieldNodeVerticleTest {

	@Before
	public void updateSchema() throws IOException {
		addNumberFieldSchema(true);
	}

	private void addNumberFieldSchema(boolean required) {
		Schema schema = schemaContainer("folder").getLatestVersion().getSchema();
		NumberFieldSchema numberFieldSchema = new NumberFieldSchemaImpl();
		numberFieldSchema.setName("numberField");
		// numberFieldSchema.setMin(10);
		// numberFieldSchema.setMax(1000);
		numberFieldSchema.setRequired(required);
		schema.removeField("numberField");
		schema.addField(numberFieldSchema);
		schemaContainer("folder").getLatestVersion().setSchema(schema);
		
	}

	@Test
	@Override
	public void testCreateNodeWithNoField() {
		addNumberFieldSchema(false);
		NodeResponse response = createNodeAndCheck("numberField", null);
		NumberFieldImpl field = response.getFields().getNumberField("numberField");
		assertNull(field);
	}

	@Test
	public void testCreateNodeWithWrongFieldType() {
		String fieldKey = "numberField";
		StringField field = new StringFieldImpl().setString("text");

		Node node = folder("2015");
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setParentNodeUuid(node.getUuid());
		nodeCreateRequest.setSchema(new SchemaReference().setName("folder"));
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.getFields().put(fieldKey, field);

		Future<NodeResponse> future = getClient().createNode(PROJECT_NAME, nodeCreateRequest, new NodeRequestParameter().setLanguages("en"));
		latchFor(future);
		expectException(future, BAD_REQUEST, "field_number_error_invalid_type", fieldKey, "text");
	}

	@Test
	@Override
	public void testUpdateNodeFieldWithField() {
		NodeResponse response = updateNode("numberField", new NumberFieldImpl().setNumber(42));
		NumberFieldImpl field = response.getFields().getNumberField("numberField");
		assertEquals(42, field.getNumber());
		response = updateNode("numberField", new NumberFieldImpl().setNumber(43));
		field = response.getFields().getNumberField("numberField");
		assertEquals(43, field.getNumber());
	}

	@Test
	@Override
	public void testCreateNodeWithField() {
		NodeResponse response = createNodeAndCheck("numberField", new NumberFieldImpl().setNumber(1.21));
		NumberFieldImpl numberField = response.getFields().getNumberField("numberField");
		assertEquals(1.21, numberField.getNumber());
	}

	@Test
	@Override
	public void testReadNodeWithExistingField() throws IOException {
		Node node = folder("2015");

		NodeGraphFieldContainer container = node.getGraphFieldContainer(english());
		NumberGraphField numberField = container.createNumber("numberField");
		numberField.setNumber(100.9f);

		NodeResponse response = readNode(node);

		NumberFieldImpl deserializedNumberField = response.getFields().getNumberField("numberField");
		assertNotNull(deserializedNumberField);
		assertEquals(100.9, deserializedNumberField.getNumber());
	}

}
