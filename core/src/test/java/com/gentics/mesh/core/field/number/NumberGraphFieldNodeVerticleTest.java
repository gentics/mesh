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
		Schema schema = schemaContainer("folder").getSchema();
		NumberFieldSchema numberFieldSchema = new NumberFieldSchemaImpl();
		numberFieldSchema.setName("numberField");
		numberFieldSchema.setMin(10);
		numberFieldSchema.setMax(1000);
		numberFieldSchema.setRequired(true);
		schema.addField(numberFieldSchema);
		schemaContainer("folder").setSchema(schema);
	}

	@Test
	@Override
	public void testCreateNodeWithNoField() {
		NodeResponse response = createNode("numberField", (Field) null);
		NumberFieldImpl field = response.getField("numberField");
		assertNotNull(field);
		assertNull(field.getNumber());
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
		if (fieldKey != null) {
			nodeCreateRequest.getFields().put(fieldKey, field);
		}

		Future<NodeResponse> future = getClient().createNode(PROJECT_NAME, nodeCreateRequest,
				new NodeRequestParameter().setLanguages("en"));
		latchFor(future);
		expectException(future, BAD_REQUEST, "error_parse_request_json_error");
	}

	@Test
	@Override
	public void testUpdateNodeFieldWithField() {
		NodeResponse response = updateNode("numberField", new NumberFieldImpl().setNumber(42));
		NumberFieldImpl field = response.getField("numberField");
		assertEquals(42, field.getNumber());
		response = updateNode("numberField", new NumberFieldImpl().setNumber(43));
		field = response.getField("numberField");
		assertEquals(43, field.getNumber());
	}

	@Test
	@Override
	public void testCreateNodeWithField() {
		NodeResponse response = createNode("numberField", new NumberFieldImpl().setNumber(1.21));
		NumberFieldImpl numberField = response.getField("numberField");
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

		NumberFieldImpl deserializedNumberField = response.getField("numberField", NumberFieldImpl.class);
		assertNotNull(deserializedNumberField);
		assertEquals(100.9, deserializedNumberField.getNumber());
	}

}
