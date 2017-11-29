package com.gentics.mesh.core.field.micronode;

import static com.gentics.mesh.test.ClientHelper.call;
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

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.list.impl.MicronodeGraphFieldListImpl;
import com.gentics.mesh.core.data.node.impl.MicronodeImpl;
import com.gentics.mesh.core.field.AbstractListFieldEndpointTest;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.MicronodeField;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.node.field.list.FieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.MicronodeFieldListImpl;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicroschemaReferenceImpl;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.syncleus.ferma.tx.Tx;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class MicronodeListFieldEndpointTest extends AbstractListFieldEndpointTest {

	protected final static String FIELD_NAME = "micronodeListField";

	@Before
	public void updateSchema() throws IOException {
		try (Tx tx = tx()) {
			SchemaModel schema = schemaContainer("folder").getLatestVersion().getSchema();
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
		Node node = folder("2015");

		NodeGraphFieldContainer container = tx(() -> node.getGraphFieldContainer("en"));
		for (int i = 0; i < 20; i++) {

			final NodeGraphFieldContainer currentContainer = container;
			List<Micronode> oldValue = tx(() -> getListValues(currentContainer, MicronodeGraphFieldListImpl.class, FIELD_NAME));
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
					NodeGraphFieldContainer newContainer = container.getNextVersion();
					assertNotNull("No new container version was created. {" + i % 3 + "}", newContainer);
					assertEquals("Check version number", newContainer.getVersion().toString(), response.getVersion());
					assertEquals("Check old value for run {" + i % 3 + "}", oldValue,
							getListValues(container, MicronodeGraphFieldListImpl.class, FIELD_NAME));
					container = newContainer;
				}
			} else {
				try (Tx tx = tx()) {
					assertEquals("Check old value for run {" + i % 3 + "}", oldValue,
							getListValues(container, MicronodeGraphFieldListImpl.class, FIELD_NAME));
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
			Node node = folder("2015");
			NodeGraphFieldContainer latest = node.getLatestDraftFieldContainer(english());
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
			FieldList<MicronodeField> field = new MicronodeFieldListImpl();
			field.add(createItem("Max", "Böse"));
			field.add(createItem("Moritz", "Böse"));
			assertThat(field.getItems()).hasSize(2);
			NodeResponse response = createNode(FIELD_NAME, field);

			FieldList<MicronodeField> responseField = response.getFields().getMicronodeFieldList(FIELD_NAME);
			assertNotNull(responseField);
			assertFieldEquals(field, responseField, true);
			assertMicronodes(responseField);
		}
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
			Set<? extends MicronodeImpl> unboundMicronodes = tx.getGraph().v().has(MicronodeImpl.class).toList(MicronodeImpl.class).stream()
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

}
