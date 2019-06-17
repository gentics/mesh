package com.gentics.mesh.core.field.node;

import static com.gentics.mesh.FieldUtil.createNodeField;
import static com.gentics.mesh.FieldUtil.createStringField;
import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_REFERENCE_UPDATED;
import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.TRACKING;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.nesting.NodeGraphField;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.field.AbstractFieldEndpointTest;
import com.gentics.mesh.core.rest.event.node.NodeMeshEventModel;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.NodeField;
import com.gentics.mesh.core.rest.node.field.impl.NodeFieldImpl;
import com.gentics.mesh.core.rest.schema.NodeFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.impl.NodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.client.DeleteParametersImpl;
import com.gentics.mesh.parameter.client.PublishParametersImpl;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(elasticsearch = TRACKING, testSize = TestSize.FULL, startServer = true)
public class NodeFieldEndpointTest extends AbstractFieldEndpointTest {

	final String FIELD_NAME = "nodeField";

	@Before
	public void updateSchema() throws Exception {
		try (Tx tx = tx()) {
			SchemaModel schema = schemaContainer("folder").getLatestVersion().getSchema();
			NodeFieldSchema nodeFieldSchema = new NodeFieldSchemaImpl();
			nodeFieldSchema.setName(FIELD_NAME);
			nodeFieldSchema.setLabel("Some label");
			nodeFieldSchema.setAllowedSchemas("folder");
			schema.addField(nodeFieldSchema);
			schemaContainer("folder").getLatestVersion().setSchema(schema);
			tx.success();
		}
	}

	@Test
	@Override
	public void testUpdateNodeFieldWithField() {
		Node node = folder("2015");
		List<Node> targetNodes = Arrays.asList(folder("news"), folder("deals"));
		for (int i = 0; i < 20; i++) {
			try (Tx tx = tx()) {
				NodeGraphFieldContainer container = node.getGraphFieldContainer("en");
				Node oldValue = getNodeValue(container, FIELD_NAME);

				Node newValue = targetNodes.get(i % 2);

				// Update the field to point to new target
				NodeResponse response = updateNode(FIELD_NAME, new NodeFieldImpl().setUuid(newValue.getUuid()));
				NodeResponse field = response.getFields().getNodeFieldExpanded(FIELD_NAME);
				assertThat(field.getUuid()).as("New Value").isEqualTo(newValue.getUuid());

				assertEquals("Check version number", container.getVersion().nextDraft().toString(), response.getVersion());
				assertEquals("Check old value", oldValue, getNodeValue(container, FIELD_NAME));
			}
		}
	}

	@Test
	@Override
	public void testUpdateSameValue() {
		try (Tx tx = tx()) {
			Node target = folder("news");
			NodeResponse firstResponse = updateNode(FIELD_NAME, new NodeFieldImpl().setUuid(target.getUuid()));
			String oldNumber = firstResponse.getVersion();

			NodeResponse secondResponse = updateNode(FIELD_NAME, new NodeFieldImpl().setUuid(target.getUuid()));
			assertThat(secondResponse.getVersion()).as("New version number").isEqualTo(oldNumber);
		}
	}

	@Test
	@Override
	public void testUpdateSetNull() {
		disableAutoPurge();

		String oldVersion;
		Node target = folder("news");

		try (Tx tx = tx()) {
			NodeResponse firstResponse = updateNode(FIELD_NAME, new NodeFieldImpl().setUuid(target.getUuid()));
			oldVersion = firstResponse.getVersion();
			tx.success();
		}

		NodeResponse secondResponse = tx(() -> updateNode(FIELD_NAME, null));
		assertThat(secondResponse.getFields().getNodeField(FIELD_NAME)).as("Deleted Field").isNull();
		assertThat(secondResponse.getVersion()).as("New version number").isNotEqualTo(oldVersion);

		try (Tx tx = tx()) {
			// Assert that the old version was not modified
			Node node = folder("2015");
			NodeGraphFieldContainer latest = node.getLatestDraftFieldContainer(english());
			assertThat(latest.getVersion().toString()).isEqualTo(secondResponse.getVersion());
			assertThat(latest.getNode(FIELD_NAME)).isNull();
			assertThat(latest.getPreviousVersion().getNode(FIELD_NAME)).isNotNull();
			String oldValue = latest.getPreviousVersion().getNode(FIELD_NAME).getNode().getUuid();
			assertThat(oldValue).isEqualTo(target.getUuid());

			NodeResponse thirdResponse = updateNode(FIELD_NAME, null);
			assertEquals("The field does not change and thus the version should not be bumped.", thirdResponse.getVersion(),
				secondResponse.getVersion());
		}
	}

	@Test
	@Override
	public void testUpdateSetEmpty() {
		String uuid = tx(() -> folder("news").getUuid());
		try (Tx tx = tx()) {
			updateNode(FIELD_NAME, new NodeFieldImpl().setUuid(uuid));
			tx.success();
		}
		try (Tx tx = tx()) {
			updateNodeFailure(FIELD_NAME, new NodeFieldImpl(), BAD_REQUEST, "node_error_field_property_missing", "uuid", FIELD_NAME);
		}
	}

	@Test
	@Override
	public void testDeleteField() {
		Node target = folder("deals");
		String targetUuid = tx(() -> target.getUuid());

		NodeResponse response = updateNode(FIELD_NAME, new NodeFieldImpl().setUuid(targetUuid));
		call(() -> client().findNodeByUuid(PROJECT_NAME, targetUuid));

		call(() -> client().deleteNode(PROJECT_NAME, response.getUuid(), new DeleteParametersImpl().setRecursive(true)));
		call(() -> client().findNodeByUuid(PROJECT_NAME, targetUuid));
	}

	/**
	 * Assert that the source node gets updated if the target is deleted.
	 */
	@Test
	public void testReferenceUpdateOnDelete() {
		String sourceUuid = tx(() -> folder("2015").getUuid());
		String targetUuid = contentUuid();

		// 1. Set the reference
		updateNode(FIELD_NAME, new NodeFieldImpl().setUuid(targetUuid));

		// 2. Publish the node so that we have to update documents (draft, published) when deleting the target
		call(() -> client().publishNode(PROJECT_NAME, sourceUuid, new PublishParametersImpl().setRecursive(true)));

		// 3. Create another draft version to add more complex data for the foreign node traversal
		NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
		nodeUpdateRequest.setLanguage("en");
		nodeUpdateRequest.setVersion("draft");
		nodeUpdateRequest.getFields().put("slug", FieldUtil.createStringField("blub123"));
		call(() -> client().updateNode(PROJECT_NAME, sourceUuid, nodeUpdateRequest));

		expect(NODE_DELETED).one();
		expect(NODE_REFERENCE_UPDATED)
			.match(1, NodeMeshEventModel.class, event -> {
				assertThat(event)
					.hasBranchUuid(initialBranchUuid())
					.hasLanguage("en")
					.hasType(DRAFT)
					.hasSchemaName("folder")
					.hasUuid(sourceUuid);
			}).match(1, NodeMeshEventModel.class, event -> {
				assertThat(event)
					.hasBranchUuid(initialBranchUuid())
					.hasLanguage("en")
					.hasType(PUBLISHED)
					.hasSchemaName("folder")
					.hasUuid(sourceUuid);
			}).two();

		call(() -> client().deleteNode(PROJECT_NAME, targetUuid));

		awaitEvents();
		waitForSearchIdleEvent();

	}

	@Test
	public void testUpdateNodeFieldWithNodeResponseJson() {
		Node node = folder("news");
		String nodeUuid = tx(() -> node.getUuid());
		Node node2 = folder("deals");
		String node2Uuid = tx(() -> node2.getUuid());
		Node updatedNode = folder("2015");
		String updatedNodeUuid = tx(() -> updatedNode.getUuid());

		// Load the node so that we can use it to prepare the update request
		NodeResponse loadedNode = call(() -> client().findNodeByUuid(PROJECT_NAME, nodeUuid, new VersioningParametersImpl().draft()));
		// Update the field to point to node
		NodeResponse response = updateNode(FIELD_NAME, loadedNode);
		NodeResponse field = response.getFields().getNodeFieldExpanded(FIELD_NAME);
		assertEquals(nodeUuid, field.getUuid());

		loadedNode = call(() -> client().findNodeByUuid(PROJECT_NAME, updatedNodeUuid, new NodeParametersImpl().setLanguages("en"),
			new VersioningParametersImpl().draft()));
		field = loadedNode.getFields().getNodeFieldExpanded(FIELD_NAME);
		assertEquals(nodeUuid, field.getUuid());

		// Update the field to point to node2
		response = updateNode(FIELD_NAME, new NodeFieldImpl().setUuid(node2Uuid));
		field = response.getFields().getNodeFieldExpanded(FIELD_NAME);
		assertEquals(node2Uuid, field.getUuid());

		loadedNode = call(() -> client().findNodeByUuid(PROJECT_NAME, updatedNodeUuid, new NodeParametersImpl().setLanguages("en"),
			new VersioningParametersImpl().draft()));
		field = loadedNode.getFields().getNodeFieldExpanded("nodeField");
		assertEquals(node2Uuid, field.getUuid());

	}

	@Test
	@Ignore("Field deletion is currently not implemented.")
	public void testCreateDeleteNodeField() {
		try (Tx tx = tx()) {
			NodeResponse response = createNode(FIELD_NAME, new NodeFieldImpl().setUuid(folder("news").getUuid()));
			NodeResponse field = response.getFields().getNodeFieldExpanded(FIELD_NAME);
			assertEquals(folder("news").getUuid(), field.getUuid());

			NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
			nodeUpdateRequest.setLanguage("en");
			nodeUpdateRequest.getFields().put(FIELD_NAME, null);

			response = client()
				.updateNode(PROJECT_NAME, response.getUuid(), nodeUpdateRequest, new NodeParametersImpl().setLanguages("en")).blockingGet();

			assertNull("The field should have been deleted", response.getFields().getNodeField(FIELD_NAME));
		}
	}

	@Test
	@Override
	public void testCreateNodeWithField() {
		try (Tx tx = tx()) {
			NodeResponse response = createNode(FIELD_NAME, new NodeFieldImpl().setUuid(folder("news").getUuid()));
			NodeResponse field = response.getFields().getNodeFieldExpanded(FIELD_NAME);
			assertEquals(folder("news").getUuid(), field.getUuid());
		}
	}

	@Test
	@Override
	public void testReadNodeWithExistingField() throws IOException {
		Node newsNode = folder("news");
		Node node = folder("2015");

		try (Tx tx = tx()) {
			NodeGraphFieldContainer container = node.getLatestDraftFieldContainer(english());
			container.createNode(FIELD_NAME, newsNode);
			tx.success();
		}

		try (Tx tx = tx()) {
			NodeResponse response = readNode(node);
			NodeField deserializedNodeField = response.getFields().getNodeField(FIELD_NAME);
			assertNotNull(deserializedNodeField);
			assertEquals(newsNode.getUuid(), deserializedNodeField.getUuid());
		}
	}

	@Test
	public void testReadNodeWithResolveLinks() {
		Node newsNode = folder("news");
		Node node = folder("2015");

		try (Tx tx = tx()) {
			NodeGraphFieldContainer container = node.getLatestDraftFieldContainer(english());
			container.createNode(FIELD_NAME, newsNode);
			tx.success();
		}

		try (Tx tx = tx()) {
			// Read the node
			NodeParametersImpl parameters = new NodeParametersImpl();
			parameters.setLanguages("en");
			parameters.setResolveLinks(LinkType.FULL);
			NodeResponse response = call(
				() -> client().findNodeByUuid(PROJECT_NAME, node.getUuid(), parameters, new VersioningParametersImpl().draft()));

			// Check whether the field contains the languagePath
			NodeField deserializedNodeField = response.getFields().getNodeField(FIELD_NAME);
			assertNotNull(deserializedNodeField);
			assertEquals(newsNode.getUuid(), deserializedNodeField.getUuid());
			assertNotNull(deserializedNodeField.getPath());
			assertNotNull(deserializedNodeField.getLanguagePaths());
			assertThat(deserializedNodeField.getLanguagePaths()).containsKeys("en", "de");
		}

	}

	@Test
	@Override
	public void testCreateNodeWithNoField() {
		try (Tx tx = tx()) {
			NodeResponse response = createNode(FIELD_NAME, (Field) null);
			NodeResponse field = response.getFields().getNodeFieldExpanded(FIELD_NAME);
			assertNull(
				"The expanded node field within the response should be null since we created the node without providing any field information.",
				field);
		}
	}

	@Test
	public void testReadNodeExpandAll() throws IOException {
		Node referencedNode = folder("news");
		Node node = folder("2015");

		try (Tx tx = tx()) {
			// Create test field
			NodeGraphFieldContainer container = node.getLatestDraftFieldContainer(english());
			container.createNode(FIELD_NAME, referencedNode);
			tx.success();
		}

		try (Tx tx = tx()) {
			NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, node.getUuid(), new NodeParametersImpl().setExpandAll(true),
				new VersioningParametersImpl().draft()));

			// Check expanded node field
			NodeResponse deserializedExpandedNodeField = response.getFields().getNodeFieldExpanded(FIELD_NAME);
			assertNotNull("The referenced field should not be null", deserializedExpandedNodeField);
			NodeResponse expandedField = (NodeResponse) deserializedExpandedNodeField;
			assertEquals(referencedNode.getUuid(), expandedField.getUuid());
			assertNotNull(expandedField.getCreator());

		}
	}

	@Test
	public void testReadNodeExpandAllNoPerm() throws IOException {
		try (Tx tx = tx()) {
			// Revoke the permission to the referenced node
			Node referencedNode = folder("news");
			role().revokePermissions(referencedNode, GraphPermission.READ_PERM);

			Node node = folder("2015");

			// Create test field
			NodeGraphFieldContainer container = node.getLatestDraftFieldContainer(english());
			container.createNode(FIELD_NAME, referencedNode);

			NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, node.getUuid(), new NodeParametersImpl().setExpandAll(true),
				new VersioningParametersImpl().draft()));

			// Assert that the field has not been loaded
			NodeResponse deserializedExpandedNodeField = response.getFields().getNodeFieldExpanded(FIELD_NAME);
			assertNull("The referenced field should be null", deserializedExpandedNodeField);

		}
	}

	@Test
	public void testReadExpandedNodeWithExistingField() throws IOException {
		Node newsNode = folder("news");
		Node node = folder("2015");

		// Create test field
		try (Tx tx = tx()) {
			NodeGraphFieldContainer container = node.getLatestDraftFieldContainer(english());
			container.createNode(FIELD_NAME, newsNode);
			tx.success();
		}

		try (Tx tx = tx()) {
			// 1. Read node with collapsed fields and check that the collapsed node field can be read
			NodeResponse responseCollapsed = readNode(node);
			NodeField deserializedNodeField = responseCollapsed.getFields().getNodeField(FIELD_NAME);
			assertNotNull(deserializedNodeField);
			assertEquals(newsNode.getUuid(), deserializedNodeField.getUuid());

			// Check whether it is possible to read the field in an expanded form.
			NodeResponse deserializedExpandedNodeField = responseCollapsed.getFields().getNodeFieldExpanded(FIELD_NAME);
			assertNotNull(deserializedExpandedNodeField);

			// 2. Read node with expanded fields
			NodeResponse responseExpanded = readNode(node, FIELD_NAME, "bogus");

			// Check collapsed node field
			deserializedNodeField = responseExpanded.getFields().getNodeField(FIELD_NAME);
			assertNotNull(deserializedNodeField);
			assertEquals(newsNode.getUuid(), deserializedNodeField.getUuid());

			// Check expanded node field
			deserializedExpandedNodeField = responseExpanded.getFields().getNodeFieldExpanded(FIELD_NAME);
			NodeResponse expandedField = (NodeResponse) deserializedExpandedNodeField;
			assertNotNull(expandedField);
			assertEquals(newsNode.getUuid(), expandedField.getUuid());
			assertNotNull(expandedField.getCreator());
		}
	}

	@Test
	public void testReadExpandedNodeWithLanguageFallback() {
		try (Tx tx = tx()) {
			Node folder = folder("2015");

			// add a node in german and english
			NodeCreateRequest createGermanNode = new NodeCreateRequest();
			createGermanNode.setSchema(new SchemaReferenceImpl().setName("folder"));
			createGermanNode.setParentNodeUuid(folder.getUuid());
			createGermanNode.setLanguage("de");
			createGermanNode.getFields().put("name", createStringField("German Target"));

			NodeResponse germanTarget = client().createNode(PROJECT_NAME, createGermanNode).blockingGet();

			NodeUpdateRequest createEnglishNode = new NodeUpdateRequest();
			createEnglishNode.setLanguage("en");
			createEnglishNode.getFields().put("name", createStringField("English Target"));

			NodeResponse updateEnglishNode = client().updateNode(PROJECT_NAME, germanTarget.getUuid(), createEnglishNode).blockingGet();

			// add a node in german (referencing the target node)
			NodeCreateRequest createSourceNode = new NodeCreateRequest();
			createSourceNode.setSchema(new SchemaReferenceImpl().setName("folder"));
			createSourceNode.setParentNodeUuid(folder.getUuid());
			createSourceNode.setLanguage("de");
			createSourceNode.getFields().put("name", createStringField("German Source"));
			createSourceNode.getFields().put(FIELD_NAME, createNodeField(germanTarget.getUuid()));

			NodeResponse source = client().createNode(PROJECT_NAME, createSourceNode).blockingGet();

			// read source node with expanded field
			for (String[] requestedLangs : asList(new String[] { "de" }, new String[] { "de", "en" }, new String[] { "en", "de" })) {
				NodeResponse response = client().findNodeByUuid(PROJECT_NAME, source.getUuid(),
					new NodeParametersImpl().setLanguages(requestedLangs).setExpandAll(true), new VersioningParametersImpl().draft()).blockingGet();
				assertEquals("Check node language", "de", response.getLanguage());
				NodeResponse nodeField = response.getFields().getNodeFieldExpanded(FIELD_NAME);
				assertNotNull("Field must be present", nodeField);
				assertEquals("Check target node language", "de", nodeField.getLanguage());
			}
		}
	}

	/**
	 * Get the node value
	 * 
	 * @param container
	 *            container
	 * @param fieldName
	 *            field name
	 * @return node value (may be null)
	 */
	protected Node getNodeValue(NodeGraphFieldContainer container, String fieldName) {
		NodeGraphField field = container.getNode(fieldName);
		return field != null ? field.getNode() : null;
	}

	@Override
	public NodeResponse createNodeWithField() {
		return createNode(FIELD_NAME, new NodeFieldImpl().setUuid(folder("news").getUuid()));
	}
}
