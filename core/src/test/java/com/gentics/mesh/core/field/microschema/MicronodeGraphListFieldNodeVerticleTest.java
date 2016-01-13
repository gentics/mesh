package com.gentics.mesh.core.field.microschema;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.elasticsearch.common.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.data.node.impl.MicronodeImpl;
import com.gentics.mesh.core.field.AbstractGraphFieldNodeVerticleTest;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.MicronodeField;
import com.gentics.mesh.core.rest.node.field.StringField;
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
		Schema schema = schemaContainer("folder").getSchema();
		ListFieldSchema listFieldSchema = new ListFieldSchemaImpl();
		listFieldSchema.setName(FIELDNAME);
		listFieldSchema.setLabel("Some label");
		listFieldSchema.setListType("micronode");
		listFieldSchema.setAllowedSchemas(new String[] {"vcard"});
		schema.addField(listFieldSchema);
		schemaContainer("folder").setSchema(schema);
		
	}

	@Test
	@Override
	public void testReadNodeWithExistingField() throws IOException {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testUpdateNodeFieldWithField() {
		FieldList<MicronodeField> field = new MicronodeFieldListImpl();
		field.add(createItem("Max", "Böse"));
		field.add(createItem("Moritz", "Böse"));

		NodeResponse response = updateNode(FIELDNAME, field);
		FieldList<MicronodeField> responseField = response.getField(FIELDNAME);
		List<String> uuids = new ArrayList<>();
		for (MicronodeField item : responseField.getItems()) {
			uuids.add(item.getUuid());
		}

		responseField.getItems().get(0).getField("firstName", StringField.class).setString("Strammer Max");
		responseField.getItems().get(1).getField("firstName", StringField.class).setString("Strammer Moritz");

		NodeResponse updateResponse = updateNode(FIELDNAME, responseField);
		FieldList<MicronodeField> updatedField = updateResponse.getField(FIELDNAME);

		assertFieldEquals(responseField, updatedField);
		assertMicronodes(updatedField);
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

		FieldList<MicronodeField> initialField = initialResponse.getField(FIELDNAME);
		FieldList<MicronodeField> reorderedField = new MicronodeFieldListImpl();
		initialField.getItems().stream().forEachOrdered(item -> reorderedField.add(item));

		Collections.sort(reorderedField.getItems(), new Comparator<MicronodeField>() {
			@Override
			public int compare(MicronodeField o1, MicronodeField o2) {
				return o1.getField("firstName", StringField.class).getString()
						.compareTo(o2.getField("firstName", StringField.class).getString());
			}
		});

		NodeResponse updateResponse = updateNode(FIELDNAME, reorderedField);
		FieldList<MicronodeField> updatedField = updateResponse.getField(FIELDNAME);

		assertFieldEquals(reorderedField, updatedField);
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
		FieldList<MicronodeField> initialField = initialResponse.getField(FIELDNAME);

		FieldList<MicronodeField> changedField = new MicronodeFieldListImpl();
		initialField.getItems().stream().forEachOrdered(item -> changedField.add(item));
		changedField.getItems().add(1, createItem("Four", "Four"));

		NodeResponse updateResponse = updateNode(FIELDNAME, changedField);
		FieldList<MicronodeField> updatedField = updateResponse.getField(FIELDNAME);
		assertFieldEquals(changedField, updatedField);
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
		FieldList<MicronodeField> initialField = initialResponse.getField(FIELDNAME);

		FieldList<MicronodeField> changedField = new MicronodeFieldListImpl();
		initialField.getItems().stream().forEachOrdered(item -> changedField.add(item));
		changedField.getItems().remove(1);

		NodeResponse updateResponse = updateNode(FIELDNAME, changedField);
		FieldList<MicronodeField> updatedField = updateResponse.getField(FIELDNAME);
		assertFieldEquals(changedField, updatedField);
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
		FieldList<MicronodeField> initialField = initialResponse.getField(FIELDNAME);

		FieldList<MicronodeField> changedField = new MicronodeFieldListImpl();
		initialField.getItems().stream().forEachOrdered(item -> changedField.add(item));
		changedField.getItems().add(createItem("Four", "Four"));
		changedField.getItems().remove(1);
		Collections.sort(changedField.getItems(), new Comparator<MicronodeField>() {
			@Override
			public int compare(MicronodeField o1, MicronodeField o2) {
				return o1.getField("firstName", StringField.class).getString()
						.compareTo(o2.getField("firstName", StringField.class).getString());
			}
		});

		NodeResponse updateResponse = updateNode(FIELDNAME, changedField);
		FieldList<MicronodeField> updatedField = updateResponse.getField(FIELDNAME);
		assertFieldEquals(changedField, updatedField);
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

		FieldList<MicronodeField> responseField = response.getField(FIELDNAME);
		assertNotNull(responseField);
		assertFieldEquals(field, responseField);
		assertMicronodes(responseField);
	}

	@Test
	@Override
	public void testCreateNodeWithNoField() {
		NodeResponse response = createNode(FIELDNAME, (Field) null);
		FieldList<MicronodeField> field = response.getField(FIELDNAME);
		assertNotNull(field);
		assertTrue("List field must be empty", field.getItems().isEmpty());
		assertMicronodes(field);
	}

	/**
	 * Create an item for the MicronodeFieldList
	 * 
	 * @param firstName first name
	 * @param lastName last name
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
	 * @param expected expected field
	 * @param field field to check
	 */
	protected void assertFieldEquals(FieldList<MicronodeField> expected, FieldList<MicronodeField> field) {
		assertEquals("Check # of micronode items", expected.getItems().size(), field.getItems().size());
		for (int i = 0; i < expected.getItems().size(); i++) {
			MicronodeField expectedMicronode = expected.getItems().get(i);
			MicronodeField micronode = field.getItems().get(i);
			for (String fieldName : Arrays.asList("firstName", "lastName")) {
				assertEquals("Check " + fieldName + " of item # " + (i + 1),
						expectedMicronode.getField(fieldName, StringField.class).getString(),
						micronode.getField(fieldName, StringField.class).getString());
			}

			if (!StringUtils.isEmpty(expectedMicronode.getUuid())) {
				assertEquals("Check uuid of item + " + (i+1), expectedMicronode.getUuid(), micronode.getUuid());
			}
		}
	}

	/**
	 * Assert that exactly the micronode instances in the given field exist in the graph db
	 * @param field field
	 */
	protected void assertMicronodes(FieldList<MicronodeField> field) {
		Set<String> existingMicronodeUuids = db.noTrx().getGraph().v().has(MicronodeImpl.class).toList(MicronodeImpl.class).stream()
				.map(micronode -> micronode.getProperty("uuid", String.class)).collect(Collectors.toSet());
		Set<String> foundMicronodeUuids = field.getItems().stream().map(MicronodeField::getUuid).collect(Collectors.toSet());

		Set<String> superFluous = new HashSet<>(existingMicronodeUuids);
		superFluous.removeAll(foundMicronodeUuids);
		assertTrue("Found superfluous micronodes: " + superFluous, superFluous.isEmpty());

		Set<String> nonExistent = new HashSet<>(foundMicronodeUuids);
		nonExistent.removeAll(existingMicronodeUuids);
		assertTrue("Found nonexistent micronodes: " + nonExistent, nonExistent.isEmpty());
	}
}
