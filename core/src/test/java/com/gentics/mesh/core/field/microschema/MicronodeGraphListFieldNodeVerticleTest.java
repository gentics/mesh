package com.gentics.mesh.core.field.microschema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.elasticsearch.common.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.list.impl.MicronodeGraphFieldListImpl;
import com.gentics.mesh.core.data.node.impl.MicronodeImpl;
import com.gentics.mesh.core.field.AbstractGraphFieldNodeVerticleTest;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.MicronodeField;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.node.field.list.FieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.MicronodeFieldListImpl;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;

public class MicronodeGraphListFieldNodeVerticleTest extends AbstractGraphFieldNodeVerticleTest {
	protected final static String FIELDNAME = "micronodeListField";

	@Before
	public void updateSchema() throws IOException {
		Schema schema = schemaContainer("folder").getLatestVersion().getSchema();
		ListFieldSchema listFieldSchema = new ListFieldSchemaImpl();
		listFieldSchema.setName(FIELDNAME);
		listFieldSchema.setLabel("Some label");
		listFieldSchema.setListType("micronode");
		listFieldSchema.setAllowedSchemas(new String[] { "vcard" });
		schema.addField(listFieldSchema);
		schemaContainer("folder").getLatestVersion().setSchema(schema);

	}

	@Test
	@Override
	public void testReadNodeWithExistingField() throws IOException {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testUpdateNodeFieldWithField() {
		Node node = folder("2015");

		for (int i = 0; i < 20; i++) {
			NodeGraphFieldContainer container = node.getGraphFieldContainer("en");
			List<Micronode> oldValue = getListValues(container, MicronodeGraphFieldListImpl.class, FIELDNAME);

			NodeResponse response = null;
			if (oldValue == null) {
				// fill with new data
				FieldList<MicronodeField> field = new MicronodeFieldListImpl();
				field.add(createItem("Max", "Böse"));
				field.add(createItem("Moritz", "Böse"));
				response = updateNode(FIELDNAME, field);

				FieldList<MicronodeField> responseField = response.getFields().getMicronodeFieldList(FIELDNAME);
				List<String> uuids = new ArrayList<>();
				for (MicronodeField item : responseField.getItems()) {
					uuids.add(item.getUuid());
				}
			} else if (i % 3 == 1) {
				// reorder data
				NodeResponse readResponse = readNode(node);
				FieldList<MicronodeField> responseField = readResponse.getFields().getMicronodeFieldList(FIELDNAME);
				Collections.reverse(responseField.getItems());

				response = updateNode(FIELDNAME, responseField);
				FieldList<MicronodeField> updatedField = response.getFields().getMicronodeFieldList(FIELDNAME);

				// compare uuids
				assertFieldEquals(responseField, updatedField, true);
			} else if (i % 3 == 2) {
				// change data
				NodeResponse readResponse = readNode(node);
				FieldList<MicronodeField> responseField = readResponse.getFields().getMicronodeFieldList(FIELDNAME);

				responseField.getItems().stream().forEach(field -> field.getFields().getStringField("firstName")
						.setString("Strammer " + field.getFields().getStringField("firstName").getString()));

				response = updateNode(FIELDNAME, responseField);
				FieldList<MicronodeField> updatedField = response.getFields().getMicronodeFieldList(FIELDNAME);

				assertFieldEquals(responseField, updatedField, false);
			} else {
				response = updateNode(FIELDNAME, new MicronodeFieldListImpl());
				assertThat(response.getFields().getMicronodeFieldList(FIELDNAME).getItems()).isEmpty();
			}

			node.reload();
			container.reload();

			assertEquals("Check version number", container.getVersion().nextDraft().toString(),
					response.getVersion().getNumber());
			assertEquals("Check old value", oldValue, getListValues(container, MicronodeGraphFieldListImpl.class, FIELDNAME));
		}
	}

	@Test
	@Override
	public void testUpdateSameValue() {
		FieldList<MicronodeField> field = new MicronodeFieldListImpl();
		field.add(createItem("Max", "Böse"));
		field.add(createItem("Moritz", "Böse"));
		NodeResponse firstResponse = updateNode(FIELDNAME, field);
		String oldNumber = firstResponse.getVersion().getNumber();

		NodeResponse secondResponse = updateNode(FIELDNAME, field);
		assertThat(secondResponse.getVersion().getNumber()).as("New version number").isEqualTo(oldNumber);
	}

	@Test
	@Override
	public void testUpdateSetNull() {
		FieldList<MicronodeField> field = new MicronodeFieldListImpl();
		field.add(createItem("Max", "Böse"));
		field.add(createItem("Moritz", "Böse"));
		NodeResponse firstResponse = updateNode(FIELDNAME, field);
		String oldNumber = firstResponse.getVersion().getNumber();

		NodeResponse secondResponse = updateNode(FIELDNAME, new MicronodeFieldListImpl());
		assertThat(secondResponse.getFields().getMicronodeFieldList(FIELDNAME)).as("Updated Field").isNotNull();
		assertThat(secondResponse.getFields().getMicronodeFieldList(FIELDNAME).getItems()).as("Updated Field Value").isEmpty();
		assertThat(secondResponse.getVersion().getNumber()).as("New version number").isNotEqualTo(oldNumber);
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
		NodeResponse initialResponse = updateNode(FIELDNAME, field);

		FieldList<MicronodeField> initialField = initialResponse.getFields().getMicronodeFieldList(FIELDNAME);
		FieldList<MicronodeField> reorderedField = new MicronodeFieldListImpl();
		initialField.getItems().stream().forEachOrdered(item -> reorderedField.add(item));

		Collections.sort(reorderedField.getItems(), new Comparator<MicronodeField>() {
			@Override
			public int compare(MicronodeField o1, MicronodeField o2) {
				return o1.getFields().getStringField("firstName").getString().compareTo(o2.getFields().getStringField("firstName").getString());
			}
		});

		NodeResponse updateResponse = updateNode(FIELDNAME, reorderedField);
		FieldList<MicronodeField> updatedField = updateResponse.getFields().getMicronodeFieldList(FIELDNAME);

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
		NodeResponse initialResponse = updateNode(FIELDNAME, field);
		FieldList<MicronodeField> initialField = initialResponse.getFields().getMicronodeFieldList(FIELDNAME);

		FieldList<MicronodeField> changedField = new MicronodeFieldListImpl();
		initialField.getItems().stream().forEachOrdered(item -> changedField.add(item));
		changedField.getItems().add(1, createItem("Four", "Four"));

		NodeResponse updateResponse = updateNode(FIELDNAME, changedField);
		FieldList<MicronodeField> updatedField = updateResponse.getFields().getMicronodeFieldList(FIELDNAME);
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
		NodeResponse initialResponse = updateNode(FIELDNAME, field);
		FieldList<MicronodeField> initialField = initialResponse.getFields().getMicronodeFieldList(FIELDNAME);

		FieldList<MicronodeField> changedField = new MicronodeFieldListImpl();
		initialField.getItems().stream().forEachOrdered(item -> changedField.add(item));
		changedField.getItems().remove(1);

		NodeResponse updateResponse = updateNode(FIELDNAME, changedField);
		FieldList<MicronodeField> updatedField = updateResponse.getFields().getMicronodeFieldList(FIELDNAME);
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
	public void testMultipleChanges() {
		FieldList<MicronodeField> field = new MicronodeFieldListImpl();
		field.add(createItem("One", "One"));
		field.add(createItem("Two", "Two"));
		field.add(createItem("Three", "Three"));
		NodeResponse initialResponse = updateNode(FIELDNAME, field);
		FieldList<MicronodeField> initialField = initialResponse.getFields().getMicronodeFieldList(FIELDNAME);

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

		NodeResponse updateResponse = updateNode(FIELDNAME, changedField);
		FieldList<MicronodeField> updatedField = updateResponse.getFields().getMicronodeFieldList(FIELDNAME);
		assertFieldEquals(changedField, updatedField, true);
		assertMicronodes(updatedField);
	}

	@Test
	@Override
	public void testCreateNodeWithField() {
		FieldList<MicronodeField> field = new MicronodeFieldListImpl();
		field.add(createItem("Max", "Böse"));
		field.add(createItem("Moritz", "Böse"));
		assertThat(field.getItems()).hasSize(2);
		NodeResponse response = createNode(FIELDNAME, field);

		FieldList<MicronodeField> responseField = response.getFields().getMicronodeFieldList(FIELDNAME);
		assertNotNull(responseField);
		assertFieldEquals(field, responseField, true);
		assertMicronodes(responseField);
	}

	@Test
	@Override
	public void testCreateNodeWithNoField() {
		NodeResponse response = createNode(FIELDNAME, (Field) null);
		FieldList<MicronodeField> field = response.getFields().getMicronodeFieldList(FIELDNAME);
		assertNotNull(field);
		assertTrue("List field must be empty", field.getItems().isEmpty());
		assertMicronodes(field);
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
		item.setMicroschema(new MicroschemaReference().setName("vcard"));
		item.getFields().put("firstName", new StringFieldImpl().setString(firstName));
		item.getFields().put("lastName", new StringFieldImpl().setString(lastName));
		return item;
	}

	/**
	 * Assert that the given fields contain the same micronodes (in the same order)
	 * 
	 * @param expected
	 *            expected field
	 * @param field
	 *            field to check
	 * @param assertUuid true to assert equality of uuids
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
		Set<? extends MicronodeImpl> unboundMicronodes = db.noTrx().getGraph().v().has(MicronodeImpl.class).toList(MicronodeImpl.class).stream()
				.filter(micronode -> micronode.getContainer() == null).collect(Collectors.toSet());
		assertThat(unboundMicronodes).as("Unbound micronodes").isEmpty();
	}
}
