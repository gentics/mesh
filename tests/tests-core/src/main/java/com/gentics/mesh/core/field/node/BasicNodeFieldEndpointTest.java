package com.gentics.mesh.core.field.node;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.core.rest.schema.HtmlFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.HtmlFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

@MeshTestSetting(testSize = FULL, startServer = true)
public class BasicNodeFieldEndpointTest extends AbstractMeshTest {

	@Test
	public void testUpdateNodeAndOmitRequiredField() throws IOException {
		try (Tx tx = tx()) {
			// 1. create required field
			HtmlFieldSchema htmlFieldSchema = new HtmlFieldSchemaImpl();
			htmlFieldSchema.setName("htmlField");
			htmlFieldSchema.setLabel("Some label");
			htmlFieldSchema.setRequired(true);
			prepareTypedSchema(schemaContainer("folder"), List.of(htmlFieldSchema), Optional.empty());

			// 2. Create new node with required field value
			HibNode parentNode = folder("2015");
			NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
			nodeCreateRequest.setParentNodeUuid(parentNode.getUuid());
			nodeCreateRequest.setSchema(new SchemaReferenceImpl().setName("folder"));
			nodeCreateRequest.setLanguage("en");
			nodeCreateRequest.getFields().put("htmlField", new HtmlFieldImpl().setHTML("Some<b>html"));

			NodeResponse response = call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest, new NodeParametersImpl().setLanguages("en")));
			assertNotNull("The response could not be found in the result of the future.", response);
			assertNotNull("The field was not included in the response.", response.getFields().getHtmlField("htmlField"));

			// 3. Update node
			NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
			nodeUpdateRequest.setLanguage("en");
			nodeUpdateRequest.setVersion("0.1");

			NodeResponse updateResponse = client()
				.updateNode(PROJECT_NAME, response.getUuid(), nodeUpdateRequest, new NodeParametersImpl().setLanguages("en")).blockingGet();
			assertNotNull("The response could not be found in the result of the future.", updateResponse);
		}
	}
}
