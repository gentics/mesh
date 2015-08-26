package com.gentics.mesh.core.field;

import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeRequestParameters;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.core.rest.schema.HtmlFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.impl.HtmlFieldSchemaImpl;
import com.gentics.mesh.core.verticle.node.ProjectNodeVerticle;
import com.gentics.mesh.demo.DemoDataProvider;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.test.AbstractRestVerticleTest;

import io.vertx.core.Future;

public class NodeFieldVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private ProjectNodeVerticle verticle;

	@Override
	public List<AbstractWebVerticle> getVertices() {
		List<AbstractWebVerticle> list = new ArrayList<>();
		list.add(verticle);
		return list;
	}

	@Test
	public void testUpdateNodeAndOmitMandatoryField() throws IOException {
		// 1. create mandatory field
		try (Trx tx = new Trx(db)) {
			Schema schema = schemaContainer("folder").getSchema();
			HtmlFieldSchema htmlFieldSchema = new HtmlFieldSchemaImpl();
			htmlFieldSchema.setName("htmlField");
			htmlFieldSchema.setLabel("Some label");
			htmlFieldSchema.setRequired(true);
			schema.addField(htmlFieldSchema);
			schemaContainer("folder").setSchema(schema);
		}

		// 2. Create new node with mandatory field value
		Node parentNode = folder("2015");
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setParentNodeUuid(parentNode.getUuid());
		nodeCreateRequest.setSchema(new SchemaReference("folder", null));
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.getFields().put("htmlField", new HtmlFieldImpl().setHTML("Some<b>html"));

		Future<NodeResponse> future = getClient().createNode(DemoDataProvider.PROJECT_NAME, nodeCreateRequest,
				new NodeRequestParameters().setLanguages("en"));
		latchFor(future);
		assertSuccess(future);
		assertNotNull("The response could not be found in the result of the future.", future.result());
		assertNotNull("The field was not included in the response.", future.result().getField("htmlField"));

		// 3. Update node
		try (Trx tx = new Trx(db)) {
			NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
			nodeUpdateRequest.setSchema(new SchemaReference("folder", null));
			nodeUpdateRequest.setLanguage("en");

			Future<NodeResponse> updateFuture = getClient().updateNode(DemoDataProvider.PROJECT_NAME, future.result().getUuid(), nodeUpdateRequest,
					new NodeRequestParameters().setLanguages("en"));
			latchFor(updateFuture);
			assertSuccess(updateFuture);
			assertNotNull("The response could not be found in the result of the future.", updateFuture.result());
		}

	}
}
