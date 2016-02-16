package com.gentics.mesh.core.schema;

import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.NumberFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.NumberFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.core.verticle.node.NodeVerticle;
import com.gentics.mesh.core.verticle.schema.SchemaVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;

import io.vertx.core.Future;

public class SchemaNodeVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private SchemaVerticle schemaVerticle;

	@Autowired
	private NodeVerticle nodeVerticle;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(nodeVerticle);
		list.add(schemaVerticle);
		return list;
	}

	@Test
	public void testUpdateAddField() throws Exception {

		Node content = content();

		SchemaContainer container = schemaContainer("content");
		Schema request = new SchemaImpl();
		request.setName("content");
		request.getFields().addAll(container.getSchema().getFields());
		StringFieldSchema nameFieldSchema = new StringFieldSchemaImpl();
		nameFieldSchema.setName("extraname");
		request.getFields().add(nameFieldSchema);

		// Update the schema client side
		Schema clientSchema = getClient().getClientSchemaStorage().getSchema("content");
		clientSchema.addField(nameFieldSchema);

		// Update the schema server side
		Future<GenericMessageResponse> future = getClient().updateSchema(container.getUuid(), request);
		latchFor(future);
		assertSuccess(future);

		// Read node and check additional field
		Future<NodeResponse> nodeFuture = getClient().findNodeByUuid(PROJECT_NAME, content.getUuid());
		latchFor(nodeFuture);
		assertSuccess(nodeFuture);
		NodeResponse response = nodeFuture.result();
		assertNotNull(response);
		assertNotNull(response.getField("extraname"));

		// Update the node and set the new field
		NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
		nodeUpdateRequest.setLanguage("en");
		nodeUpdateRequest.setSchema(new SchemaReference().setName("content"));
		nodeUpdateRequest.getFields().put("extraname", new StringFieldImpl().setString("sometext"));
		nodeFuture = getClient().updateNode(PROJECT_NAME, content.getUuid(), nodeUpdateRequest);
		latchFor(nodeFuture);
		assertSuccess(nodeFuture);
		response = nodeFuture.result();
		assertNotNull(response);
		assertNotNull(response.getField("extraname"));
		assertEquals("sometext", ((StringFieldImpl) response.getField("extraname")).getString());

	}

	@Test
	public void testRemoveFieldType() {
		Node content = content();

		SchemaContainer schema = schemaContainer("content");
		Schema request = new SchemaImpl();
		request.setName("content");
		for (FieldSchema fieldSchema : schema.getSchema().getFields()) {
			if ("title".equals(fieldSchema.getName())) {
				continue;
			}
			request.getFields().add(fieldSchema);
		}

		// Update the schema client side
		Schema clientSchema = getClient().getClientSchemaStorage().getSchema("content");
		getClient().getClientSchemaStorage().removeSchema("content");
		clientSchema.removeField("title");
		getClient().getClientSchemaStorage().addSchema(clientSchema);

		// Update the schema server side
		Future<GenericMessageResponse> future = getClient().updateSchema(schema.getUuid(), request);
		latchFor(future);
		assertSuccess(future);

		// Read node and check additional field
		Future<NodeResponse> nodeFuture = getClient().findNodeByUuid(PROJECT_NAME, content.getUuid());
		latchFor(nodeFuture);
		assertSuccess(nodeFuture);
		NodeResponse response = nodeFuture.result();
		assertNotNull(response);
		assertNull(response.getField("title"));

	}

	@Test
	public void testRemoveAddFieldTypeWithSameKey() {

		Node content = content();

		// 1. Remove title field from schema
		SchemaContainer schema = schemaContainer("content");
		Schema request = new SchemaImpl();
		request.setName("content");
		for (FieldSchema fieldSchema : schema.getSchema().getFields()) {
			if ("title".equals(fieldSchema.getName())) {
				continue;
			}
			request.getFields().add(fieldSchema);
		}
		// 2. Add title field with different type
		NumberFieldSchema titleFieldSchema = new NumberFieldSchemaImpl();
		titleFieldSchema.setName("title");
		request.addField(titleFieldSchema);

		// Update the schema client side
		Schema clientSchema = getClient().getClientSchemaStorage().getSchema("content");
		clientSchema.addField(titleFieldSchema);

		// Update the schema server side
		Future<GenericMessageResponse> future = getClient().updateSchema(schema.getUuid(), request);
		latchFor(future);
		assertSuccess(future);

		// Read node and check additional field
		Future<NodeResponse> nodeFuture = getClient().findNodeByUuid(PROJECT_NAME, content.getUuid());
		latchFor(nodeFuture);
		assertSuccess(nodeFuture);
		NodeResponse response = nodeFuture.result();
		assertNotNull(response);
		assertNotNull(response.getField("title"));
		assertEquals(NumberFieldImpl.class, response.getField("title").getClass());

		// Update the node and set the new field
		NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
		nodeUpdateRequest.setLanguage("en");
		nodeUpdateRequest.setSchema(new SchemaReference().setName("content"));
		nodeUpdateRequest.getFields().put("title", new NumberFieldImpl().setNumber(42.01));
		nodeFuture = getClient().updateNode(PROJECT_NAME, content.getUuid(), nodeUpdateRequest);
		latchFor(nodeFuture);
		assertSuccess(nodeFuture);
		response = nodeFuture.result();
		assertNotNull(response);
		assertNotNull(response.getField("title"));
		assertEquals(42.01, ((NumberFieldImpl) response.getField("title")).getNumber());

	}
}
