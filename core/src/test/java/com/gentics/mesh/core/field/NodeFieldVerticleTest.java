package com.gentics.mesh.core.field;

import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.VersionReference;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.core.rest.schema.HtmlFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.impl.HtmlFieldSchemaImpl;
import com.gentics.mesh.core.verticle.node.NodeVerticle;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.impl.NodeParameters;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.AbstractIsolatedRestVerticleTest;

public class NodeFieldVerticleTest extends AbstractIsolatedRestVerticleTest {

	@Autowired
	private NodeVerticle verticle;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(verticle);
		return list;
	}

	@Test
	public void testUpdateNodeAndOmitRequiredField() throws IOException {
		try (NoTx noTx = db.noTx()) {
			// 1. create required field
			Schema schema = schemaContainer("folder").getLatestVersion().getSchema();
			HtmlFieldSchema htmlFieldSchema = new HtmlFieldSchemaImpl();
			htmlFieldSchema.setName("htmlField");
			htmlFieldSchema.setLabel("Some label");
			htmlFieldSchema.setRequired(true);
			schema.addField(htmlFieldSchema);
			schemaContainer("folder").getLatestVersion().setSchema(schema);

			// 2. Create new node with required field value
			Node parentNode = folder("2015");
			NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
			nodeCreateRequest.setParentNodeUuid(parentNode.getUuid());
			nodeCreateRequest.setSchema(new SchemaReference().setName("folder"));
			nodeCreateRequest.setLanguage("en");
			nodeCreateRequest.getFields().put("htmlField", new HtmlFieldImpl().setHTML("Some<b>html"));

			MeshResponse<NodeResponse> future = getClient().createNode(PROJECT_NAME, nodeCreateRequest, new NodeParameters().setLanguages("en")).invoke();
			latchFor(future);
			assertSuccess(future);
			assertNotNull("The response could not be found in the result of the future.", future.result());
			assertNotNull("The field was not included in the response.", future.result().getFields().getHtmlField("htmlField"));

			// 3. Update node
			NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
			nodeUpdateRequest.setSchema(new SchemaReference().setName("folder"));
			nodeUpdateRequest.setLanguage("en");
			nodeUpdateRequest.setVersion(new VersionReference().setNumber("0.1"));

			MeshResponse<NodeResponse> updateFuture = getClient().updateNode(PROJECT_NAME, future.result().getUuid(), nodeUpdateRequest,
					new NodeParameters().setLanguages("en")).invoke();
			latchFor(updateFuture);
			assertSuccess(updateFuture);
			assertNotNull("The response could not be found in the result of the future.", updateFuture.result());
		}
	}
}
