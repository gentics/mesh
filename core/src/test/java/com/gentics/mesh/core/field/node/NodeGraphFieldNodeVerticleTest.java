package com.gentics.mesh.core.field.node;

import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.field.AbstractGraphFieldNodeVerticleTest;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.NodeField;
import com.gentics.mesh.core.rest.node.field.impl.NodeFieldImpl;
import com.gentics.mesh.core.rest.schema.NodeFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.impl.NodeFieldSchemaImpl;
import com.gentics.mesh.query.impl.NodeRequestParameter;
import com.gentics.mesh.util.FieldUtil;

import io.vertx.core.Future;

public class NodeGraphFieldNodeVerticleTest extends AbstractGraphFieldNodeVerticleTest {

	final String NODE_FIELD_NAME = "nodeField";

	@Before
	public void updateSchema() throws Exception {
		Schema schema = schemaContainer("folder").getLatestVersion().getSchema();
		NodeFieldSchema nodeFieldSchema = new NodeFieldSchemaImpl();
		nodeFieldSchema.setName(NODE_FIELD_NAME);
		nodeFieldSchema.setLabel("Some label");
		nodeFieldSchema.setAllowedSchemas("folder");
		schema.addField(nodeFieldSchema);
		schemaContainer("folder").getLatestVersion().setSchema(schema);
	}

	@Test
	@Override
	public void testUpdateNodeFieldWithField() {
		Node node = folder("news");
		Node node2 = folder("deals");

		// Update the field to point to node
		NodeResponse response = updateNode(NODE_FIELD_NAME, new NodeFieldImpl().setUuid(node.getUuid()));
		NodeResponse field = response.getFields().getNodeFieldExpanded(NODE_FIELD_NAME);
		assertEquals("We updated the node field in node 2015 but the response did not contain the expected node reference value", node.getUuid(),
				field.getUuid());

		// Update the field to point to node2
		response = updateNode("nodeField", new NodeFieldImpl().setUuid(node2.getUuid()));
		field = response.getFields().getNodeFieldExpanded("nodeField");
		assertEquals("We updated the node field in node 2015 but the response did not contain the expected node2 reference value", node2.getUuid(),
				field.getUuid());
	}

	@Test
	public void testUpdateNodeFieldWithNodeResponseJson() {
		Node node = folder("news");
		Node node2 = folder("deals");

		Node updatedNode = folder("2015");
		// Load the node so that we can use it to prepare the update request
		Future<NodeResponse> future = getClient().findNodeByUuid(PROJECT_NAME, node.getUuid());
		latchFor(future);
		NodeResponse loadedNode = future.result();

		// Update the field to point to node
		NodeResponse response = updateNode(NODE_FIELD_NAME, loadedNode);
		NodeResponse field = response.getFields().getNodeFieldExpanded(NODE_FIELD_NAME);
		assertEquals(node.getUuid(), field.getUuid());

		Future<NodeResponse> loadedNodeFuture = getClient().findNodeByUuid(PROJECT_NAME, updatedNode.getUuid(),
				new NodeRequestParameter().setLanguages("en"));
		latchFor(loadedNodeFuture);
		assertSuccess(loadedNodeFuture);
		field = loadedNodeFuture.result().getFields().getNodeFieldExpanded(NODE_FIELD_NAME);
		assertEquals(node.getUuid(), field.getUuid());

		// Update the field to point to node2
		response = updateNode(NODE_FIELD_NAME, new NodeFieldImpl().setUuid(node2.getUuid()));
		field = response.getFields().getNodeFieldExpanded(NODE_FIELD_NAME);
		assertEquals(node2.getUuid(), field.getUuid());

		loadedNodeFuture = getClient().findNodeByUuid(PROJECT_NAME, updatedNode.getUuid(), new NodeRequestParameter().setLanguages("en"));
		latchFor(loadedNodeFuture);
		assertSuccess(loadedNodeFuture);
		field = loadedNodeFuture.result().getFields().getNodeFieldExpanded("nodeField");
		assertEquals(node2.getUuid(), field.getUuid());

	}

	@Test
	@Ignore("Field deletion is currently not implemented.")
	public void testCreateDeleteNodeField() {

		NodeResponse response = createNode(NODE_FIELD_NAME, new NodeFieldImpl().setUuid(folder("news").getUuid()));
		NodeResponse field = response.getFields().getNodeFieldExpanded(NODE_FIELD_NAME);
		assertEquals(folder("news").getUuid(), field.getUuid());

		NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
		nodeUpdateRequest.setSchema(new SchemaReference().setName("folder"));
		nodeUpdateRequest.setLanguage("en");
		nodeUpdateRequest.getFields().put(NODE_FIELD_NAME, null);

		Future<NodeResponse> future = getClient().updateNode(PROJECT_NAME, response.getUuid(), nodeUpdateRequest,
				new NodeRequestParameter().setLanguages("en"));
		latchFor(future);
		assertSuccess(future);
		response = future.result();

		assertNull("The field should have been deleted", response.getFields().getNodeField(NODE_FIELD_NAME));
	}

	@Test
	@Override
	public void testCreateNodeWithField() {
		NodeResponse response = createNode(NODE_FIELD_NAME, new NodeFieldImpl().setUuid(folder("news").getUuid()));
		NodeResponse field = response.getFields().getNodeFieldExpanded(NODE_FIELD_NAME);
		assertEquals(folder("news").getUuid(), field.getUuid());
	}

	@Test
	@Override
	public void testReadNodeWithExistingField() throws IOException {
		Node newsNode = folder("news");
		Node node = folder("2015");

		NodeGraphFieldContainer container = node.getGraphFieldContainer(english());
		container.createNode(NODE_FIELD_NAME, newsNode);
		NodeResponse response = readNode(node);
		NodeField deserializedNodeField = response.getFields().getNodeField(NODE_FIELD_NAME);
		assertNotNull(deserializedNodeField);
		assertEquals(newsNode.getUuid(), deserializedNodeField.getUuid());
	}

	@Test
	@Override
	public void testCreateNodeWithNoField() {
		NodeResponse response = createNode(NODE_FIELD_NAME, (Field) null);
		NodeResponse field = response.getFields().getNodeFieldExpanded(NODE_FIELD_NAME);
		assertNotNull(field);
		assertNull(field.getUuid());
	}

	@Test
	public void testReadNodeExpandAll() throws IOException {
		Node newsNode = folder("news");
		Node node = folder("2015");

		// Create test field
		NodeGraphFieldContainer container = node.getGraphFieldContainer(english());
		container.createNode(NODE_FIELD_NAME, newsNode);

		Future<NodeResponse> future = getClient().findNodeByUuid(PROJECT_NAME, node.getUuid(), new NodeRequestParameter().setExpandAll(true));
		latchFor(future);
		assertSuccess(future);

		// Check expanded node field
		NodeResponse deserializedExpandedNodeField = future.result().getFields().getNodeFieldExpanded(NODE_FIELD_NAME);
		assertNotNull("The ", deserializedExpandedNodeField);
		NodeResponse expandedField = (NodeResponse) deserializedExpandedNodeField;
		assertEquals(newsNode.getUuid(), expandedField.getUuid());
		assertNotNull(expandedField.getCreator());

	}

	@Test
	public void testReadExpandedNodeWithExistingField() throws IOException {
		Node newsNode = folder("news");
		Node node = folder("2015");

		// Create test field
		NodeGraphFieldContainer container = node.getGraphFieldContainer(english());
		container.createNode(NODE_FIELD_NAME, newsNode);

		// 1. Read node with collapsed fields and check that the collapsed node field can be read
		NodeResponse responseCollapsed = readNode(node);
		NodeField deserializedNodeField = responseCollapsed.getFields().getNodeField(NODE_FIELD_NAME);
		assertNotNull(deserializedNodeField);
		assertEquals(newsNode.getUuid(), deserializedNodeField.getUuid());

		// Check whether it is possible to read the field in an expanded form.
		NodeResponse deserializedExpandedNodeField = responseCollapsed.getFields().getNodeFieldExpanded(NODE_FIELD_NAME);
		assertNotNull(deserializedExpandedNodeField);

		// 2. Read node with expanded fields
		NodeResponse responseExpanded = readNode(node, NODE_FIELD_NAME, "bogus");

		// Check collapsed node field
		deserializedNodeField = responseExpanded.getFields().getNodeField(NODE_FIELD_NAME);
		assertNotNull(deserializedNodeField);
		assertEquals(newsNode.getUuid(), deserializedNodeField.getUuid());

		// Check expanded node field
		deserializedExpandedNodeField = responseExpanded.getFields().getNodeFieldExpanded(NODE_FIELD_NAME);
		NodeResponse expandedField = (NodeResponse) deserializedExpandedNodeField;
		assertNotNull(expandedField);
		assertEquals(newsNode.getUuid(), expandedField.getUuid());
		assertNotNull(expandedField.getCreator());
	}

	@Test
	public void testReadExpandedNodeWithLanguageFallback() {
		Node folder = folder("2015");

		// add a node in german and english
		NodeCreateRequest createGermanNode = new NodeCreateRequest();
		createGermanNode.setSchema(new SchemaReference().setName("folder"));
		createGermanNode.setParentNodeUuid(folder.getUuid());
		createGermanNode.setLanguage("de");
		createGermanNode.getFields().put("name", FieldUtil.createStringField("German Target"));

		Future<NodeResponse> createGermanFuture = getClient().createNode(PROJECT_NAME, createGermanNode);
		latchFor(createGermanFuture);
		assertSuccess(createGermanFuture);
		NodeResponse germanTarget = createGermanFuture.result();

		NodeUpdateRequest createEnglishNode = new NodeUpdateRequest();
		createEnglishNode.setSchema(new SchemaReference().setName("folder"));
		createEnglishNode.setLanguage("en");
		createEnglishNode.getFields().put("name", FieldUtil.createStringField("English Target"));

		Future<NodeResponse> updateEnglishNode = getClient().updateNode(PROJECT_NAME, germanTarget.getUuid(), createEnglishNode);
		latchFor(updateEnglishNode);
		assertSuccess(updateEnglishNode);

		// add a node in german (referencing the target node)
		NodeCreateRequest createSourceNode = new NodeCreateRequest();
		createSourceNode.setSchema(new SchemaReference().setName("folder"));
		createSourceNode.setParentNodeUuid(folder.getUuid());
		createSourceNode.setLanguage("de");
		createSourceNode.getFields().put("name", FieldUtil.createStringField("German Source"));
		createSourceNode.getFields().put(NODE_FIELD_NAME, FieldUtil.createNodeField(germanTarget.getUuid()));

		Future<NodeResponse> createSourceFuture = getClient().createNode(PROJECT_NAME, createSourceNode);
		latchFor(createSourceFuture);
		assertSuccess(createSourceFuture);
		NodeResponse source = createSourceFuture.result();

		// read source node with expanded field
		for (String[] requestedLangs : Arrays.asList(new String[] { "de" }, new String[] { "de", "en" },
				new String[] { "en", "de" })) {
			Future<NodeResponse> resultFuture = getClient().findNodeByUuid(PROJECT_NAME, source.getUuid(),
					new NodeRequestParameter().setLanguages(requestedLangs).setExpandAll(true));
			latchFor(resultFuture);
			assertSuccess(resultFuture);
			NodeResponse response = resultFuture.result();
			assertEquals("Check node language", "de", response.getLanguage());
			NodeResponse nodeField = response.getFields().getNodeFieldExpanded(NODE_FIELD_NAME);
			assertNotNull("Field must be present", nodeField);
			assertEquals("Check target node language", "de", nodeField.getLanguage());
		}
	}
}
