package com.gentics.mesh.core.field.list;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.list.impl.StringGraphFieldListImpl;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NumberFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;

public class GraphListFieldStringVerticleTest extends AbstractGraphListFieldVerticleTest {

	@Override
	String getListFieldType() {
		return "string";
	}

	@Test
	public void testCreateNodeWithNullFieldValue() throws IOException {
		NodeResponse response = createNode(FIELD_NAME, (Field) null);
		StringFieldListImpl nodeField = response.getFields().getStringFieldList(FIELD_NAME);
		assertNotNull(nodeField);
		assertEquals(0, nodeField.getItems().size());
	}

	@Test
	public void testCreateEmptyStringList() throws IOException {
		StringFieldListImpl listField = new StringFieldListImpl();
		NodeResponse response = createNode(FIELD_NAME, listField);
		StringFieldListImpl listFromResponse = response.getFields().getStringFieldList(FIELD_NAME);
		assertEquals(0, listFromResponse.getItems().size());
	}

	@Test
	public void testCreateNullStringList() throws IOException {
		StringFieldListImpl listField = new StringFieldListImpl();
		listField.setItems(null);
		NodeResponse response = createNode(FIELD_NAME, listField);
		StringFieldListImpl listFromResponse = response.getFields().getStringFieldList(FIELD_NAME);
		assertEquals(0, listFromResponse.getItems().size());
	}

	@Test
	public void testCreateWithOmittedStringListValue() throws IOException {
		NodeResponse response = createNode(null, (Field) null);
		StringFieldListImpl listFromResponse = response.getFields().getStringFieldList(FIELD_NAME);
		assertNotNull(listFromResponse);
		assertEquals(0, listFromResponse.getItems().size());
	}

	@Test
	public void testStringList() throws IOException {
		StringFieldListImpl listField = new StringFieldListImpl();
		listField.add("A");
		listField.add("B");
		listField.add("C");

		NodeResponse response = createNode(FIELD_NAME, listField);
		StringFieldListImpl listFromResponse = response.getFields().getStringFieldList(FIELD_NAME);
		assertEquals(3, listFromResponse.getItems().size());
		assertEquals(Arrays.asList("A", "B", "C").toString(), listFromResponse.getItems().toString());
	}

	@Test
	public void testUpdateNodeWithStringField() throws IOException {
		Node node = folder("2015");

		List<List<String>> valueCombinations = Arrays.asList(Arrays.asList("A", "B", "C"), Arrays.asList("C", "B", "A"), Collections.emptyList(),
				Arrays.asList("X", "Y"), Arrays.asList("C"));

		NodeGraphFieldContainer container = node.getGraphFieldContainer("en");
		for (int i = 0; i < 20; i++) {
			List<String> oldValue = getListValues(container, StringGraphFieldListImpl.class, FIELD_NAME);
			List<String> newValue = valueCombinations.get(i % valueCombinations.size());
			System.out.println("OLD: " + oldValue);
			System.out.println("NEW: " + newValue);

			StringFieldListImpl list = new StringFieldListImpl();
			for (String value : newValue) {
				list.add(value);
			}
			NodeResponse response = updateNode(FIELD_NAME, list);
			StringFieldListImpl field = response.getFields().getStringFieldList(FIELD_NAME);
			assertThat(field.getItems()).as("Updated field").containsExactlyElementsOf(list.getItems());
			node.reload();
			container.reload();

			NodeGraphFieldContainer newContainerVersion = container.getNextVersion();
			assertEquals("Check version number", newContainerVersion.getVersion().toString(), response.getVersion().getNumber());
			//assertEquals("Check old value", oldValue, getListValues(container, StringGraphFieldListImpl.class, FIELD_NAME));
			container = newContainerVersion;
		}
	}

	@Test
	@Override
	public void testUpdateSetNull() {
		StringFieldListImpl list = new StringFieldListImpl();
		list.add("A");
		list.add("B");
		updateNode(FIELD_NAME, list);

		NodeResponse secondResponse = updateNode(FIELD_NAME, null);
		assertThat(secondResponse.getFields().getStringFieldList(FIELD_NAME)).as("Updated Field").isNull();
	}

	@Test
	public void testUpdateSetEmpty() {
		StringFieldListImpl list = new StringFieldListImpl();
		list.add("A");
		list.add("B");
		updateNode(FIELD_NAME, list);

		NodeResponse secondResponse = updateNode(FIELD_NAME, new StringFieldListImpl());
		assertThat(secondResponse.getFields().getStringFieldList(FIELD_NAME)).as("Updated field list").isNotNull();
		assertThat(secondResponse.getFields().getStringFieldList(FIELD_NAME).getItems()).as("Field value should be truncated").isEmpty();
	}

}
