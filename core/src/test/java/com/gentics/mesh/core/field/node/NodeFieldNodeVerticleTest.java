package com.gentics.mesh.core.field.node;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import io.vertx.core.Future;

import java.io.IOException;

import org.apache.commons.lang.NotImplementedException;
import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.field.AbstractFieldNodeVerticleTest;
import com.gentics.mesh.core.rest.node.NodeRequestParameters;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.NodeField;
import com.gentics.mesh.core.rest.node.field.impl.NodeFieldImpl;
import com.gentics.mesh.core.rest.schema.NodeFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.NodeFieldSchemaImpl;
import com.gentics.mesh.demo.DemoDataProvider;

public class NodeFieldNodeVerticleTest extends AbstractFieldNodeVerticleTest {

	@Before
	public void updateSchema() throws IOException {
		Schema schema = schemaContainer("folder").getSchema();
		NodeFieldSchema nodeFieldSchema = new NodeFieldSchemaImpl();
		nodeFieldSchema.setName("nodeField");
		nodeFieldSchema.setLabel("Some label");
		nodeFieldSchema.setAllowedSchemas("folder");
		schema.addField(nodeFieldSchema);
		schemaContainer("folder").setSchema(schema);
	}

	@Test
	@Override
	public void testUpdateNodeFieldWithField() {
		throw new NotImplementedException();
	}

	@Test
	@Override
	public void testUpdateNodeFieldWithNoField() {
		throw new NotImplementedException();
	}

	@Test
	@Override
	public void testCreateNodeWithField() {
		throw new NotImplementedException();
	}

	@Test
	@Override
	public void testReadNodeWithExitingField() throws IOException {
		Node newsNode = folder("news");
		Node node = folder("2015");

		NodeFieldContainer container = node.getFieldContainer(english());
		container.createNode("nodeField", newsNode);

		NodeRequestParameters parameters = new NodeRequestParameters();
		parameters.setLanguages("de");
		Future<NodeResponse> future = getClient().findNodeByUuid(DemoDataProvider.PROJECT_NAME, node.getUuid(), parameters);
		latchFor(future);
		assertSuccess(future);

		NodeField deserializedNodeField = future.result().getField("nodeField", NodeFieldImpl.class);
		assertNotNull(deserializedNodeField);
		assertEquals(newsNode.getUuid(), deserializedNodeField.getUuid());
	}
}
