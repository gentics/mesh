package com.gentics.mesh.core.field.micronode;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_REFERENCE_UPDATED;
import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.TRACKING;
import static com.gentics.mesh.util.DateUtils.toISO8601;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeGraphField;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.field.AbstractFieldEndpointTest;
import com.gentics.mesh.core.rest.event.node.NodeMeshEventModel;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.microschema.MicroschemaModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaUpdateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.MicronodeField;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.node.field.impl.NodeFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListItemImpl;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.impl.BooleanFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.DateFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.HtmlFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicroschemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.NodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NumberFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.client.PublishParametersImpl;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.syncleus.ferma.tx.Tx;

@MeshTestSetting(elasticsearch = TRACKING, testSize = FULL, startServer = true)
public class MicronodeFieldEndpointTest extends AbstractFieldEndpointTest {

	protected final static String FIELD_NAME = "micronodeField";

	@Before
	public void updateSchema() throws IOException {
		try (Tx tx = tx()) {
			SchemaModel schema = schemaContainer("folder").getLatestVersion().getSchema();
			MicronodeFieldSchema microschemaFieldSchema = new MicronodeFieldSchemaImpl();
			microschemaFieldSchema.setName(FIELD_NAME);
			microschemaFieldSchema.setLabel("Some label");
			microschemaFieldSchema.setAllowedMicroSchemas(new String[] { "vcard" });
			schema.addField(microschemaFieldSchema);
			schemaContainer("folder").getLatestVersion().setSchema(schema);
			tx.success();
		}
	}

	@Test
	@Override
	public void testCreateNodeWithNoField() {
		try (Tx tx = tx()) {
			NodeResponse response = createNode(FIELD_NAME, (Field) null);
			MicronodeField field = response.getFields().getMicronodeField(FIELD_NAME);
			assertNull(field);
		}
	}

	@Test
	@Override
	public void testUpdateNodeFieldWithField() {
		Node node = folder("2015");

		MicronodeResponse field = new MicronodeResponse();
		field.setMicroschema(new MicroschemaReferenceImpl().setName("vcard"));
		field.getFields().put("firstName", new StringFieldImpl().setString("Max"));
		field.getFields().put("lastName", new StringFieldImpl().setString("Moritz"));
		updateNode(FIELD_NAME, field);

		for (int i = 0; i < 20; i++) {
			String newLastName = "Moritz" + i;
			Micronode oldValue = null;
			NodeGraphFieldContainer container = null;
			try (Tx tx = tx()) {
				container = node.getGraphFieldContainer("en");
				oldValue = getMicronodeValue(container, FIELD_NAME);
				field = new MicronodeResponse();
				field.setMicroschema(new MicroschemaReferenceImpl().setName("vcard"));
				field.getFields().put("lastName", new StringFieldImpl().setString(newLastName));
			}

			NodeResponse response = updateNode(FIELD_NAME, field);

			try (Tx tx = tx()) {
				MicronodeResponse fieldResponse = response.getFields().getMicronodeField(FIELD_NAME);
				assertThat(fieldResponse).hasStringField("firstName", "Max").hasStringField("lastName", newLastName);

				container.getNextVersions().iterator().next();
				assertEquals("Check version number", container.getVersion().nextDraft().toString(), response.getVersion());
				if (oldValue == null) {
					assertThat(getMicronodeValue(container, FIELD_NAME)).as("old value").isNull();
				} else {
					assertThat(oldValue.getString("lastName").getString()).as("old lastName").isNotEqualTo(newLastName);
					assertThat(getMicronodeValue(container, FIELD_NAME)).as("old value").isEqualToComparingFieldByField(oldValue);
					assertThat(fieldResponse.getUuid()).as("New uuid").isNotEqualTo(oldValue.getUuid());
				}
			}
		}
	}

	@Test
	@Override
	public void testUpdateSameValue() {
		try (Tx tx = tx()) {
			MicronodeResponse field = new MicronodeResponse();
			field.setMicroschema(new MicroschemaReferenceImpl().setName("vcard"));
			field.getFields().put("firstName", new StringFieldImpl().setString("Max"));
			field.getFields().put("lastName", new StringFieldImpl().setString("Moritz"));
			NodeResponse firstResponse = updateNode(FIELD_NAME, field);
			String oldNumber = firstResponse.getVersion();

			NodeResponse secondResponse = updateNode(FIELD_NAME, field);
			assertThat(secondResponse.getVersion()).as("New version number").isEqualTo(oldNumber);
		}
	}

	@Test
	@Override
	public void testUpdateSetNull() {
		MicronodeResponse field = new MicronodeResponse();
		field.setMicroschema(new MicroschemaReferenceImpl().setName("vcard"));
		field.getFields().put("firstName", new StringFieldImpl().setString("Max"));
		field.getFields().put("lastName", new StringFieldImpl().setString("Moritz"));
		NodeResponse firstResponse = updateNode(FIELD_NAME, field);
		String oldNumber = firstResponse.getVersion();

		// Assert that a null field value request will delete the micronode
		NodeResponse secondResponse = updateNode(FIELD_NAME, null);
		try (Tx tx = tx()) {
			assertThat(secondResponse.getFields().getMicronodeField(FIELD_NAME)).isNull();
			assertThat(secondResponse.getVersion()).as("New version number").isNotEqualTo(oldNumber);

			// Assert that the old version was not modified
			Node node = folder("2015");
			NodeGraphFieldContainer latest = node.getLatestDraftFieldContainer(english());
			assertThat(latest.getVersion().toString()).isEqualTo(secondResponse.getVersion());
			assertThat(latest.getMicronode(FIELD_NAME)).isNull();
			assertThat(latest.getPreviousVersion().getMicronode(FIELD_NAME)).as("The old version micronode field could not be found.").isNotNull();
			Micronode oldMicronode = latest.getPreviousVersion().getMicronode(FIELD_NAME).getMicronode();
			assertThat(oldMicronode.getString("firstName").getString()).as("Old version micronode firstname field value should not be modified")
				.isEqualTo("Max");

			NodeResponse thirdResponse = updateNode(FIELD_NAME, null);
			assertEquals("The field does not change and thus the version should not be bumped.", thirdResponse.getVersion(),
				secondResponse.getVersion());
		}
	}

	@Test
	@Override
	public void testUpdateSetEmpty() {
		try (Tx tx = tx()) {
			MicronodeResponse field = new MicronodeResponse();
			field.setMicroschema(new MicroschemaReferenceImpl().setName("vcard"));
			field.getFields().put("firstName", new StringFieldImpl().setString("Max"));
			field.getFields().put("lastName", new StringFieldImpl().setString("Moritz"));
			NodeResponse firstResponse = updateNode(FIELD_NAME, field);
			String oldVersion = firstResponse.getVersion();

			createNodeAndExpectFailure(FIELD_NAME, new MicronodeResponse(), BAD_REQUEST, "micronode_error_missing_reference", "micronodeField");

			MicronodeResponse emptyField = new MicronodeResponse().setMicroschema(new MicroschemaReferenceImpl().setName("vcard"));

			// Assert that an empty request will not update any data of the micronode
			NodeResponse secondResponse = updateNode(FIELD_NAME, emptyField);
			assertThat(secondResponse.getFields().getMicronodeField(FIELD_NAME)).as("Updated Field").isNotNull();
			assertThat(secondResponse.getFields().getMicronodeField(FIELD_NAME).getFields().getStringField("firstName").getString()).isEqualTo("Max");
			assertThat(secondResponse.getFields().getMicronodeField(FIELD_NAME).getFields().getStringField("lastName").getString())
				.isEqualTo("Moritz");
			assertThat(secondResponse.getVersion()).as("No new version number should have been generated").isEqualTo(oldVersion);

			NodeResponse thirdResponse = updateNode(FIELD_NAME, emptyField);
			assertEquals("The field does not change and thus the version should not be bumped.", thirdResponse.getVersion(),
				secondResponse.getVersion());
		}
	}

	@Test
	@Override
	public void testCreateNodeWithField() {
		try (Tx tx = tx()) {
			NodeResponse response = createNodeWithField();

			MicronodeResponse createdField = response.getFields().getMicronodeField(FIELD_NAME);
			assertNotNull("Created field does not exist", createdField);
			assertNotNull("Micronode has no uuid set", createdField.getUuid());

			assertEquals("Check microschema name", "vcard", createdField.getMicroschema().getName());
			assertEquals("Check microschema uuid", microschemaContainers().get("vcard").getUuid(), createdField.getMicroschema().getUuid());

			// check micronode fields
			StringField createdFirstnameField = createdField.getFields().getStringField("firstName");
			assertNotNull("Micronode did not contain firstName field", createdFirstnameField);
			assertEquals("Check micronode firstName", "Max", createdFirstnameField.getString());
		}
	}

	/**
	 * Assert that the source node gets updated if the target is deleted.
	 */
	@Test
	public void testReferenceUpdateOnDelete() {
		String sourceUuid = tx(() -> folder("2015").getUuid());
		String targetUuid = contentUuid();

		String vcardUuid = tx(() -> microschemaContainers().get("vcard").getUuid());
		MicroschemaModel vcard = tx(() -> microschemaContainers().get("vcard").getLatestVersion().getSchema());
		vcard.addField(new NodeFieldSchemaImpl().setName("node"));
		MicroschemaUpdateRequest request = JsonUtil.readValue(vcard.toJson(), MicroschemaUpdateRequest.class);
		call(() -> client().updateMicroschema(vcardUuid, request));

		// 1. Set the reference
		MicronodeResponse field = new MicronodeResponse();
		field.setMicroschema(new MicroschemaReferenceImpl().setName("vcard"));
		field.getFields().put("firstName", new StringFieldImpl().setString("Max"));
		field.getFields().put("lastName", new StringFieldImpl().setString("Moritz"));
		field.getFields().put("node", new NodeFieldImpl().setUuid(targetUuid));
		updateNode(FIELD_NAME, field);

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

	/**
	 * Assert that the source node gets updated if the target is deleted.
	 */
	@Test
	public void testReferenceListUpdateOnDelete() {
		String sourceUuid = tx(() -> folder("2015").getUuid());
		String targetUuid = contentUuid();

		String vcardUuid = tx(() -> microschemaContainers().get("vcard").getUuid());
		MicroschemaModel vcard = tx(() -> microschemaContainers().get("vcard").getLatestVersion().getSchema());
		vcard.addField(new ListFieldSchemaImpl().setListType("node").setName("node"));
		MicroschemaUpdateRequest request = JsonUtil.readValue(vcard.toJson(), MicroschemaUpdateRequest.class);
		call(() -> client().updateMicroschema(vcardUuid, request));

		// 1. Set the reference
		MicronodeResponse field = new MicronodeResponse();
		field.setMicroschema(new MicroschemaReferenceImpl().setName("vcard"));
		field.getFields().put("firstName", new StringFieldImpl().setString("Max"));
		field.getFields().put("lastName", new StringFieldImpl().setString("Moritz"));

		NodeFieldListImpl nodeList = new NodeFieldListImpl();
		nodeList.add(new NodeFieldListItemImpl().setUuid(targetUuid));
		field.getFields().put("node", nodeList);
		updateNode(FIELD_NAME, field);

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
			
		})
		.two();

		call(() -> client().deleteNode(PROJECT_NAME, targetUuid));

		awaitEvents();
		waitForSearchIdleEvent();

	}

	@Test
	public void testCreateNodeWithInvalidMicroschema() {
		try (Tx tx = tx()) {
			MicronodeResponse field = new MicronodeResponse();
			MicroschemaReferenceImpl microschema = new MicroschemaReferenceImpl();
			microschema.setName("notexisting");
			field.setMicroschema(microschema);
			field.getFields().put("firstName", new StringFieldImpl().setString("Max"));
			createNodeAndExpectFailure(FIELD_NAME, field, BAD_REQUEST, "error_microschema_reference_not_found", "notexisting", "-", "-");
		}
	}

	@Test
	public void testCreateNodeWithNotAllowedMicroschema() {
		try (Tx tx = tx()) {
			MicronodeResponse field = new MicronodeResponse();
			MicroschemaReferenceImpl microschema = new MicroschemaReferenceImpl();
			microschema.setName("captionedImage");
			field.setMicroschema(microschema);
			field.getFields().put("firstName", new StringFieldImpl().setString("Max"));
			createNodeAndExpectFailure(FIELD_NAME, field, BAD_REQUEST, "node_error_invalid_microschema_field_value", "micronodeField",
				"captionedImage");
		}
	}

	@Test
	@Override
	public void testReadNodeWithExistingField() throws IOException {
		Node node = folder("2015");
		try (Tx tx = tx()) {
			MicroschemaContainerVersion microschema = microschemaContainers().get("vcard").getLatestVersion();
			NodeGraphFieldContainer container = node.getLatestDraftFieldContainer(english());
			MicronodeGraphField micronodeField = container.createMicronode(FIELD_NAME, microschema);
			micronodeField.getMicronode().createString("firstName").setString("Max");
			tx.success();
		}

		try (Tx tx = tx()) {
			NodeResponse response = readNode(node);
			MicronodeResponse deserializedMicronodeField = response.getFields().getMicronodeField(FIELD_NAME);
			assertNotNull("Micronode field must not be null", deserializedMicronodeField);
			StringField firstNameField = deserializedMicronodeField.getFields().getStringField("firstName");
			assertNotNull("Micronode must contain firstName field", firstNameField);
			assertEquals("Check firstName value", "Max", firstNameField.getString());
		}

	}

	/**
	 * Test reading a node which has a micronode field which has a node field that reference the node we are currently reading.
	 * 
	 * We expect that resolving still works even if the expandAll flag is set.
	 */
	@Test
	public void testExpandAllCyclicMicronodeWithNodeReference() {
		Node node = folder("2015");
		MicroschemaModel nodeMicroschema = new MicroschemaModelImpl();

		try (Tx tx = tx()) {
			// 1. Create microschema noderef with nodefield
			nodeMicroschema.setName("noderef");
			for (int i = 0; i < 10; i++) {
				nodeMicroschema.addField(new NodeFieldSchemaImpl().setName("nodefield_" + i));
			}
			microschemaContainers().put("noderef",
				project().getMicroschemaContainerRoot().create(nodeMicroschema, getRequestUser(), EventQueueBatch.create()));

			// 2. Update the folder schema and add a micronode field
			SchemaModel schema = schemaContainer("folder").getLatestVersion().getSchema();
			MicronodeFieldSchema microschemaFieldSchema = new MicronodeFieldSchemaImpl();
			microschemaFieldSchema.setName("noderef");
			microschemaFieldSchema.setLabel("Micronode field");
			microschemaFieldSchema.setAllowedMicroSchemas(new String[] { "noderef" });
			schema.addField(microschemaFieldSchema);
			schemaContainer("folder").getLatestVersion().setSchema(schema);
			tx.success();
		}

		// 3. Update the node
		try (Tx tx = tx()) {
			MicronodeResponse field = new MicronodeResponse();
			field.setMicroschema(new MicroschemaReferenceImpl().setName("noderef"));
			for (int i = 0; i < 10; i++) {
				field.getFields().put("nodefield_" + i, FieldUtil.createNodeField(node.getUuid()));
			}
			NodeResponse response = updateNode("noderef", field, true);
			assertThat(response.getFields().getMicronodeField("noderef")).matches(field, nodeMicroschema);
		}
	}

	/**
	 * Test updating a node with a micronode containing all possible field types
	 * 
	 * @throws IOException
	 */
	@Test
	public void testUpdateFieldTypes() throws IOException {
		Long date = System.currentTimeMillis();
		Node newsOverview = content("news overview");
		Node newsFolder = folder("news");
		MicroschemaModel fullMicroschema = new MicroschemaModelImpl();

		try (Tx tx = tx()) {

			// 1. Create microschema that includes all field types

			fullMicroschema.setName("full");
			// TODO implement BinaryField in Micronode
			// fullMicroschema.addField(new BinaryFieldSchemaImpl().setName("binaryfield").setLabel("Binary Field"));
			fullMicroschema.addField(new BooleanFieldSchemaImpl().setName("booleanfield").setLabel("Boolean Field"));
			fullMicroschema.addField(new DateFieldSchemaImpl().setName("datefield").setLabel("Date Field"));
			fullMicroschema.addField(new HtmlFieldSchemaImpl().setName("htmlfield").setLabel("HTML Field"));
			// TODO implement BinaryField in Micronode
			// fullMicroschema.addField(new ListFieldSchemaImpl().setListType("binary").setName("listfield-binary").setLabel("Binary List Field"));
			fullMicroschema.addField(new ListFieldSchemaImpl().setListType("boolean").setName("listfield-boolean").setLabel("Boolean List Field"));
			fullMicroschema.addField(new ListFieldSchemaImpl().setListType("date").setName("listfield-date").setLabel("Date List Field"));
			fullMicroschema.addField(new ListFieldSchemaImpl().setListType("html").setName("listfield-html").setLabel("Html List Field"));
			fullMicroschema.addField(new ListFieldSchemaImpl().setListType("node").setName("listfield-node").setLabel("Node List Field"));
			fullMicroschema.addField(new ListFieldSchemaImpl().setListType("number").setName("listfield-number").setLabel("Number List Field"));
			fullMicroschema.addField(new ListFieldSchemaImpl().setListType("string").setName("listfield-string").setLabel("String List Field"));
			fullMicroschema.addField(new NodeFieldSchemaImpl().setName("nodefield").setLabel("Node Field"));
			fullMicroschema.addField(new NumberFieldSchemaImpl().setName("numberfield").setLabel("Number Field"));
			fullMicroschema.addField(new StringFieldSchemaImpl().setName("stringfield").setLabel("String Field"));

			// 2. Add the microschema to the list of microschemas of the project
			microschemaContainers().put("full",
				project().getMicroschemaContainerRoot().create(fullMicroschema, getRequestUser(), EventQueueBatch.create()));

			// 3. Update the folder schema and inject a micronode field which uses the full schema
			SchemaModel schema = schemaContainer("folder").getLatestVersion().getSchema();
			MicronodeFieldSchema microschemaFieldSchema = new MicronodeFieldSchemaImpl();
			microschemaFieldSchema.setName("full");
			microschemaFieldSchema.setLabel("Micronode field");
			microschemaFieldSchema.setAllowedMicroSchemas(new String[] { "full" });
			schema.addField(microschemaFieldSchema);
			schemaContainer("folder").getLatestVersion().setSchema(schema);
			tx.success();
		}

		// 4. Prepare the micronode field for the update request
		MicronodeResponse field = new MicronodeResponse();
		try (Tx tx = tx()) {
			field.setMicroschema(new MicroschemaReferenceImpl().setName("full"));
			field.getFields().put("booleanfield", FieldUtil.createBooleanField(true));
			field.getFields().put("datefield", FieldUtil.createDateField(toISO8601(date)));
			field.getFields().put("htmlfield", FieldUtil.createHtmlField("<b>HTML</b> value"));
			field.getFields().put("listfield-boolean", FieldUtil.createBooleanListField(true, false));
			field.getFields().put("listfield-date", FieldUtil.createDateListField(toISO8601(date), toISO8601(0)));
			field.getFields().put("listfield-html", FieldUtil.createHtmlListField("<b>first</b>", "<i>second</i>", "<u>third</u>"));
			field.getFields().put("listfield-node", FieldUtil.createNodeListField(newsOverview.getUuid(), newsFolder.getUuid()));
			field.getFields().put("listfield-number", FieldUtil.createNumberListField(47, 11));
			field.getFields().put("listfield-string", FieldUtil.createStringListField("first", "second", "third"));
			field.getFields().put("nodefield", FieldUtil.createNodeField(newsOverview.getUuid()));
			field.getFields().put("numberfield", FieldUtil.createNumberField(4711));
			field.getFields().put("stringfield", FieldUtil.createStringField("String value"));
		}

		try (Tx tx = tx()) {
			// 5. Invoke the update request
			NodeResponse response = updateNode("full", field);

			// 6. Compare the response with the update request
			assertThat(response.getFields().getMicronodeField("full")).matches(field, fullMicroschema);
		}

	}

	/**
	 * Get the micronode value
	 * 
	 * @param container
	 *            container
	 * @param fieldName
	 *            field name
	 * @return micronode value or null
	 */
	protected Micronode getMicronodeValue(NodeGraphFieldContainer container, String fieldName) {
		MicronodeGraphField field = container.getMicronode(fieldName);
		return field != null ? field.getMicronode() : null;
	}

	@Override
	public NodeResponse createNodeWithField() {
		MicronodeResponse field = new MicronodeResponse();
		MicroschemaReferenceImpl microschema = new MicroschemaReferenceImpl();
		microschema.setName("vcard");
		field.setMicroschema(microschema);
		field.getFields().put("firstName", new StringFieldImpl().setString("Max"));
		field.getFields().put("lastName", new StringFieldImpl().setString("Mustermann"));
		return createNode(FIELD_NAME, field);
	}

}
