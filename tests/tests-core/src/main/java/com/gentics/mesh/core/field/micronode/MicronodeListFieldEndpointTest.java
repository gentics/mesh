package com.gentics.mesh.core.field.micronode;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_REFERENCE_UPDATED;
import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.ElasticsearchTestMode.TRACKING;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.node.HibMicronode;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.field.AbstractListFieldEndpointTest;
import com.gentics.mesh.core.rest.event.node.NodeMeshEventModel;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaUpdateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.MicronodeField;
import com.gentics.mesh.core.rest.node.field.impl.NodeFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.node.field.list.FieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.MicronodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListItemImpl;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicroschemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.NodeFieldSchemaImpl;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.client.PublishParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;

@MeshTestSetting(elasticsearch = TRACKING, testSize = FULL, startServer = true)
public class MicronodeListFieldEndpointTest extends AbstractListFieldEndpointTest {

	protected final static String FIELD_NAME = "micronodeListField";

	@Before
	public void updateSchema() throws IOException {
		try (Tx tx = tx()) {
			SchemaVersionModel schema = schemaContainer("folder").getLatestVersion().getSchema();
			ListFieldSchema listFieldSchema = new ListFieldSchemaImpl();
			listFieldSchema.setName(FIELD_NAME);
			listFieldSchema.setLabel("Some label");
			listFieldSchema.setListType("micronode");
			listFieldSchema.setAllowedSchemas(new String[] { "vcard" });
			schema.addField(listFieldSchema);
			schemaContainer("folder").getLatestVersion().setSchema(schema);
		}
	}

	@Override
	public String getListFieldType() {
		return "micronode";
	}

	@Test
	@Override
	@Ignore("Not yet implemented")
	public void testReadNodeWithExistingField() throws IOException {
	}

	@Test
	@Override
	public void testNullValueInListOnCreate() {
		try (Tx tx = tx()) {
			FieldList<MicronodeField> listField = new MicronodeFieldListImpl();
			listField.add(createItem("Max", "Böse"));
			listField.add(null);
			createNodeAndExpectFailure(FIELD_NAME, listField, BAD_REQUEST, "field_list_error_null_not_allowed", FIELD_NAME);
		}
	}

	@Test
	@Override
	public void testNullValueInListOnUpdate() {
		try (Tx tx = tx()) {
			FieldList<MicronodeField> listField = new MicronodeFieldListImpl();
			listField.add(createItem("Max", "Böse"));
			listField.add(null);
			updateNodeFailure(FIELD_NAME, listField, BAD_REQUEST, "field_list_error_null_not_allowed", FIELD_NAME);
		}
	}

	@Test
	@Override
	public void testUpdateNodeFieldWithField() {
		disableAutoPurge();
		HibNode node = folder("2015");

		HibNodeFieldContainer container = tx(() -> boot().contentDao().getFieldContainer(node, "en"));
		for (int i = 0; i < 20; i++) {

			final HibNodeFieldContainer currentContainer = container;
			List<HibMicronode> oldValue = tx(() -> getListValues(currentContainer::getMicronodeList, FIELD_NAME));
			FieldList<MicronodeField> newValue = new MicronodeFieldListImpl();
			NodeResponse response = null;
			if (oldValue == null) {
				// fill with new data
				newValue.getItems().clear();
				newValue.add(createItem("Max", "Böse"));
				newValue.add(createItem("Moritz", "Böse"));
				response = updateNode(FIELD_NAME, newValue);

				FieldList<MicronodeField> responseField = response.getFields().getMicronodeFieldList(FIELD_NAME);
				List<String> uuids = new ArrayList<>();
				for (MicronodeField item : responseField.getItems()) {
					uuids.add(item.getUuid());
				}
			} else if (i % 3 == 1) {
				// reorder data
				NodeResponse readResponse = readNode(node);
				newValue = readResponse.getFields().getMicronodeFieldList(FIELD_NAME);
				Collections.reverse(newValue.getItems());

				response = updateNode(FIELD_NAME, newValue);
				FieldList<MicronodeField> updatedField = response.getFields().getMicronodeFieldList(FIELD_NAME);

				// compare uuids
				assertFieldEquals(newValue, updatedField, true);
			} else if (i % 3 == 2) {
				// change data
				NodeResponse readResponse = readNode(node);
				newValue = readResponse.getFields().getMicronodeFieldList(FIELD_NAME);

				for (MicronodeField field : newValue.getItems()) {
					StringFieldImpl firstNameField = field.getFields().getStringField("firstName");
					firstNameField.setString("Strammer " + firstNameField.getString());
					field.getFields().put("firstName", firstNameField);
				}
				response = updateNode(FIELD_NAME, newValue);
				FieldList<MicronodeField> updatedField = response.getFields().getMicronodeFieldList(FIELD_NAME);
				assertFieldEquals(newValue, updatedField, false);
			} else {
				response = updateNode(FIELD_NAME, new MicronodeFieldListImpl());
				assertThat(response.getFields().getMicronodeFieldList(FIELD_NAME).getItems()).isEmpty();
			}

			// We only have to check for new versions if those should have been created.
			boolean bothEmpty = oldValue != null && newValue.getItems().isEmpty() && oldValue.isEmpty();
			if (!bothEmpty) {
				try (Tx tx = tx()) {
					ContentDao contentDao = tx.contentDao();
					HibNodeFieldContainer newContainer = contentDao.getNextVersions(container).iterator().next();
					assertNotNull("No new container version was created. {" + i % 3 + "}", newContainer);
					assertEquals("Check version number", newContainer.getVersion().toString(), response.getVersion());
					assertEquals("Check old value for run {" + i % 3 + "}", oldValue,
						getListValues(container::getMicronodeList, FIELD_NAME));
					container = newContainer;
				}
			} else {
				try (Tx tx = tx()) {
					assertEquals("Check old value for run {" + i % 3 + "}", oldValue,
						getListValues(container::getMicronodeList, FIELD_NAME));
					assertEquals("The version should not have been updated.", container.getVersion().toString(), response.getVersion());
				}
			}
		}
	}

	@Test
	@Override
	public void testUpdateSameValue() {
		try (Tx tx = tx()) {
			FieldList<MicronodeField> field = new MicronodeFieldListImpl();
			field.add(createItem("Max", "Böse"));
			field.add(createItem("Moritz", "Böse"));
			NodeResponse firstResponse = updateNode(FIELD_NAME, field);
			String oldNumber = firstResponse.getVersion();

			NodeResponse secondResponse = updateNode(FIELD_NAME, field);
			assertThat(secondResponse.getVersion()).as("New version number").isEqualTo(oldNumber);
		}
	}

	@Test
	@Override
	public void testUpdateSetNull() {
		disableAutoPurge();

		FieldList<MicronodeField> field = new MicronodeFieldListImpl();
		field.add(createItem("Max", "Böse"));
		field.add(createItem("Moritz", "Böse"));
		NodeResponse firstResponse = updateNode(FIELD_NAME, field);
		String oldVersion = firstResponse.getVersion();

		NodeResponse secondResponse = updateNode(FIELD_NAME, null);
		assertThat(secondResponse.getFields().getMicronodeFieldList(FIELD_NAME)).as("Updated Field").isNull();
		assertThat(secondResponse.getVersion()).as("New version number").isNotEqualTo(oldVersion);
		assertThat(oldVersion).as("Version should be updated").isNotEqualTo(secondResponse.getVersion());

		// Assert that the old version was not modified
		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();
			HibNode node = folder("2015");
			HibNodeFieldContainer latest = contentDao.getLatestDraftFieldContainer(node, english());
			assertThat(latest.getVersion().toString()).isEqualTo(secondResponse.getVersion());
			assertThat(latest.getMicronodeList(FIELD_NAME)).isNull();
			assertThat(latest.getPreviousVersion().getMicronodeList(FIELD_NAME)).isNotNull();
			List<String> oldValueList = latest.getPreviousVersion().getMicronodeList(FIELD_NAME).getList().stream()
				.map(item -> item.getMicronode().getString("firstName").getString()).collect(Collectors.toList());
			assertThat(oldValueList).containsExactly("Max", "Moritz");

			NodeResponse thirdResponse = updateNode(FIELD_NAME, null);
			assertEquals("The field does not change and thus the version should not be bumped.", thirdResponse.getVersion(),
				secondResponse.getVersion());
		}
	}

	@Test
	@Override
	public void testUpdateSetEmpty() {
		FieldList<MicronodeField> field = new MicronodeFieldListImpl();
		field.add(createItem("Max", "Böse"));
		field.add(createItem("Moritz", "Böse"));
		NodeResponse firstResponse = updateNode(FIELD_NAME, field);
		String oldVersion = firstResponse.getVersion();

		MicronodeFieldListImpl emptyField = new MicronodeFieldListImpl();
		NodeResponse secondResponse = updateNode(FIELD_NAME, emptyField);
		assertThat(secondResponse.getFields().getMicronodeFieldList(FIELD_NAME)).as("Updated Field").isNotNull();
		assertThat(secondResponse.getFields().getMicronodeFieldList(FIELD_NAME).getItems()).as("Updated Field Value").isEmpty();
		assertThat(secondResponse.getVersion()).as("New version number").isNotEqualTo(oldVersion);

		NodeResponse thirdResponse = updateNode(FIELD_NAME, emptyField);
		assertEquals("The field does not change and thus the version should not be bumped.", thirdResponse.getVersion(), secondResponse.getVersion());
		assertThat(secondResponse.getVersion()).as("No new version number should be generated").isEqualTo(secondResponse.getVersion());
	}

	/**
	 * Test reordering micronodes in the list
	 */
	@Test
	public void testReorder() {
		FieldList<MicronodeField> field = new MicronodeFieldListImpl();
		field.add(createItem("One", "One"));
		field.add(createItem("Two", "Two"));
		field.add(createItem("Three", "Three"));
		NodeResponse initialResponse = updateNode(FIELD_NAME, field);

		FieldList<MicronodeField> initialField = initialResponse.getFields().getMicronodeFieldList(FIELD_NAME);
		FieldList<MicronodeField> reorderedField = new MicronodeFieldListImpl();
		initialField.getItems().stream().forEachOrdered(item -> reorderedField.add(item));

		Collections.sort(reorderedField.getItems(), new Comparator<MicronodeField>() {
			@Override
			public int compare(MicronodeField o1, MicronodeField o2) {
				return o1.getFields().getStringField("firstName").getString().compareTo(o2.getFields().getStringField("firstName").getString());
			}
		});

		NodeResponse updateResponse = updateNode(FIELD_NAME, reorderedField);
		FieldList<MicronodeField> updatedField = updateResponse.getFields().getMicronodeFieldList(FIELD_NAME);

		assertFieldEquals(reorderedField, updatedField, true);
		assertMicronodes(updatedField);
	}

	/**
	 * Test adding a micronode to the list
	 */
	@Test
	public void testAddMicronode() {
		FieldList<MicronodeField> field = new MicronodeFieldListImpl();
		field.add(createItem("One", "One"));
		field.add(createItem("Two", "Two"));
		field.add(createItem("Three", "Three"));
		NodeResponse initialResponse = updateNode(FIELD_NAME, field);
		FieldList<MicronodeField> initialField = initialResponse.getFields().getMicronodeFieldList(FIELD_NAME);

		FieldList<MicronodeField> changedField = new MicronodeFieldListImpl();
		initialField.getItems().stream().forEachOrdered(item -> changedField.add(item));
		changedField.getItems().add(1, createItem("Four", "Four"));

		NodeResponse updateResponse = updateNode(FIELD_NAME, changedField);
		FieldList<MicronodeField> updatedField = updateResponse.getFields().getMicronodeFieldList(FIELD_NAME);
		assertFieldEquals(changedField, updatedField, true);
		assertMicronodes(updatedField);
	}

	/**
	 * Test removing a micronode from the list
	 */
	@Test
	public void testRemoveMicronode() {
		FieldList<MicronodeField> field = new MicronodeFieldListImpl();
		field.add(createItem("One", "One"));
		field.add(createItem("Two", "Two"));
		field.add(createItem("Three", "Three"));
		NodeResponse initialResponse = updateNode(FIELD_NAME, field);
		FieldList<MicronodeField> initialField = initialResponse.getFields().getMicronodeFieldList(FIELD_NAME);

		FieldList<MicronodeField> changedField = new MicronodeFieldListImpl();
		initialField.getItems().stream().forEachOrdered(item -> changedField.add(item));
		changedField.getItems().remove(1);

		NodeResponse updateResponse = updateNode(FIELD_NAME, changedField);
		FieldList<MicronodeField> updatedField = updateResponse.getFields().getMicronodeFieldList(FIELD_NAME);
		assertFieldEquals(changedField, updatedField, true);
		assertMicronodes(updatedField);
	}

	/**
	 * Test doing multiple changes to the list at once
	 * <ol>
	 * <li>Remove an element</li>
	 * <li>Add an element</li>
	 * <li>Reorder remaining elements</li>
	 * </ol>
	 */
	@Test
	@Ignore
	public void testMultipleChanges() {
		try (Tx tx = tx()) {
			FieldList<MicronodeField> field = new MicronodeFieldListImpl();
			field.add(createItem("One", "One"));
			field.add(createItem("Two", "Two"));
			field.add(createItem("Three", "Three"));
			NodeResponse initialResponse = updateNode(FIELD_NAME, field);
			FieldList<MicronodeField> initialField = initialResponse.getFields().getMicronodeFieldList(FIELD_NAME);

			FieldList<MicronodeField> changedField = new MicronodeFieldListImpl();
			initialField.getItems().stream().forEachOrdered(item -> changedField.add(item));
			changedField.getItems().add(createItem("Four", "Four"));
			changedField.getItems().remove(1);
			Collections.sort(changedField.getItems(), new Comparator<MicronodeField>() {
				@Override
				public int compare(MicronodeField o1, MicronodeField o2) {
					return o1.getFields().getStringField("firstName").getString().compareTo(o2.getFields().getStringField("firstName").getString());
				}
			});

			NodeResponse updateResponse = updateNode(FIELD_NAME, changedField);
			FieldList<MicronodeField> updatedField = updateResponse.getFields().getMicronodeFieldList(FIELD_NAME);
			assertFieldEquals(changedField, updatedField, true);
			assertMicronodes(updatedField);
		}
	}

	@Test
	@Override
	public void testCreateNodeWithField() {
		try (Tx tx = tx()) {
			// 1. Create the node
			FieldList<MicronodeField> field = new MicronodeFieldListImpl();
			field.add(createItem("Max", "Böse"));
			field.add(createItem("Moritz", "Böse"));
			assertThat(field.getItems()).hasSize(2);
			NodeResponse response = createNode(FIELD_NAME, field);

			// Assert the response
			FieldList<MicronodeField> responseField = response.getFields().getMicronodeFieldList(FIELD_NAME);
			assertNotNull(responseField);
			assertFieldEquals(field, responseField, true);
			assertMicronodes(responseField);
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
		MicroschemaVersionModel vcard = tx(() -> microschemaContainers().get("vcard").getLatestVersion().getSchema());
		vcard.addField(new NodeFieldSchemaImpl().setName("node"));
		MicroschemaUpdateRequest request = JsonUtil.readValue(vcard.toJson(), MicroschemaUpdateRequest.class);
		call(() -> client().updateMicroschema(vcardUuid, request));

		// 1. Set the reference
		MicronodeResponse fieldItem = new MicronodeResponse();
		fieldItem.setMicroschema(new MicroschemaReferenceImpl().setName("vcard"));
		fieldItem.getFields().put("firstName", new StringFieldImpl().setString("Max"));
		fieldItem.getFields().put("lastName", new StringFieldImpl().setString("Moritz"));
		fieldItem.getFields().put("node", new NodeFieldImpl().setUuid(targetUuid));

		FieldList<MicronodeField> field = new MicronodeFieldListImpl();
		field.add(fieldItem);
		field.add(fieldItem);
		field.add(fieldItem);
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

	/**
	 * Assert that the source node gets updated if the target is deleted.
	 */
	@Test
	public void testReferenceListUpdateOnDelete() {
		String sourceUuid = tx(() -> folder("2015").getUuid());
		String targetUuid = contentUuid();

		String vcardUuid = tx(() -> microschemaContainers().get("vcard").getUuid());
		MicroschemaVersionModel vcard = tx(() -> microschemaContainers().get("vcard").getLatestVersion().getSchema());
		vcard.addField(new ListFieldSchemaImpl().setListType("node").setName("node"));
		MicroschemaUpdateRequest request = JsonUtil.readValue(vcard.toJson(), MicroschemaUpdateRequest.class);
		call(() -> client().updateMicroschema(vcardUuid, request));

		// 1. Set the reference
		MicronodeResponse fieldItem = new MicronodeResponse();
		fieldItem.setMicroschema(new MicroschemaReferenceImpl().setName("vcard"));
		fieldItem.getFields().put("firstName", new StringFieldImpl().setString("Max"));
		fieldItem.getFields().put("lastName", new StringFieldImpl().setString("Moritz"));

		NodeFieldListImpl nodeList = new NodeFieldListImpl();
		nodeList.add(new NodeFieldListItemImpl().setUuid(targetUuid));
		fieldItem.getFields().put("node", nodeList);

		FieldList<MicronodeField> field = new MicronodeFieldListImpl();
		field.add(fieldItem);
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
	@Override
	public void testCreateNodeWithNoField() {
		try (Tx tx = tx()) {
			NodeResponse response = createNode(FIELD_NAME, (Field) null);
			FieldList<MicronodeField> field = response.getFields().getMicronodeFieldList(FIELD_NAME);
			assertNull(field);
		}
	}

	/**
	 * Covers issue #84 (https://github.com/gentics/mesh/issues/84)
	 */
	@Test
	public void testListOrder() {
		int nodeCount = 50;
		FieldList<MicronodeField> field = new MicronodeFieldListImpl();
		String[] expected = new String[nodeCount];
		for (int i = 0; i < nodeCount; i++) {
			String name = "name" + i;
			field.add(createItem(name, name));
			expected[i] = name;
		}
		NodeResponse node = updateNode(FIELD_NAME, field);

		NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, node.getUuid()));

		Stream<String> actual = response.getFields().getMicronodeFieldList(FIELD_NAME).getItems().stream()
			.map(it -> it.getFields().getStringField("firstName").getString());

		assertThat(actual).containsExactly(expected);
	}

	/**
	 * Assert that the given fields contain the same micronodes (in the same order)
	 *
	 * @param expected
	 *            expected field
	 * @param field
	 *            field to check
	 * @param assertUuid
	 *            true to assert equality of uuids
	 */
	protected void assertFieldEquals(FieldList<MicronodeField> expected, FieldList<MicronodeField> field, boolean assertUuid) {
		assertEquals("Check # of micronode items", expected.getItems().size(), field.getItems().size());
		for (int i = 0; i < expected.getItems().size(); i++) {
			MicronodeField expectedMicronode = expected.getItems().get(i);
			MicronodeField micronode = field.getItems().get(i);
			for (String fieldName : Arrays.asList("firstName", "lastName")) {
				assertEquals("Check " + fieldName + " of item # " + (i + 1), expectedMicronode.getFields().getStringField(fieldName).getString(),
					micronode.getFields().getStringField(fieldName).getString());
			}

			// TODO enable comparing uuids
			if (false && assertUuid && !StringUtils.isEmpty(expectedMicronode.getUuid())) {
				assertEquals("Check uuid of item + " + (i + 1), expectedMicronode.getUuid(), micronode.getUuid());
			}
		}
	}

	/**
	 * Assert that all micronodes are bound to field containers
	 *
	 * @param field
	 *            field
	 */
	protected void assertMicronodes(FieldList<MicronodeField> field) {
		try (Tx tx = tx()) {
			CommonTx ctx = tx.unwrap();
			TraversalResult<? extends HibMicronode> s = new TraversalResult<>(ctx.contentDao().findAllMicronodes());
			Set<? extends HibMicronode> unboundMicronodes = s.stream()
				.filter(micronode -> micronode.getContainer() == null).collect(Collectors.toSet());
			assertThat(unboundMicronodes).as("Unbound micronodes").isEmpty();
		}
	}

	/**
	 * Create an item for the MicronodeFieldList
	 *
	 * @param firstName
	 *            first name
	 * @param lastName
	 *            last name
	 * @return item
	 */
	protected MicronodeResponse createItem(String firstName, String lastName) {
		MicronodeResponse item = new MicronodeResponse();
		item.setMicroschema(new MicroschemaReferenceImpl().setName("vcard"));
		item.getFields().put("firstName", new StringFieldImpl().setString(firstName));
		item.getFields().put("lastName", new StringFieldImpl().setString(lastName));
		return item;
	}

	@Override
	public NodeResponse createNodeWithField() {
		FieldList<MicronodeField> field = new MicronodeFieldListImpl();
		field.add(createItem("Max", "Böse"));
		field.add(createItem("Moritz", "Böse"));
		assertThat(field.getItems()).hasSize(2);
		return createNode(FIELD_NAME, field);
	}

}
