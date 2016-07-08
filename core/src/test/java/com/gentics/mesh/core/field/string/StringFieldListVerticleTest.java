package com.gentics.mesh.core.field.string;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.list.impl.StringGraphFieldListImpl;
import com.gentics.mesh.core.field.AbstractGraphListFieldVerticleTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;

public class StringFieldListVerticleTest extends AbstractGraphListFieldVerticleTest {

	@Override
	public String getListFieldType() {
		return "string";
	}

	@Test
	public void testCreateNodeWithNullFieldValue() throws IOException {
		NodeResponse response = createNode(FIELD_NAME, (Field) null);
		StringFieldListImpl nodeField = response.getFields().getStringFieldList(FIELD_NAME);
		assertNull("No string field should have been created.", nodeField);
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
		assertNull("The string list should be null since the request was sending null instead of an array.", listFromResponse);
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
			assertEquals("The old container version did not match", container.getVersion().nextDraft().toString(), response.getVersion().toString());
			assertEquals("Check version number", newContainerVersion.getVersion().toString(), response.getVersion().getNumber());
			assertEquals("Check old value", oldValue, getListValues(container, StringGraphFieldListImpl.class, FIELD_NAME));
			assertEquals("Check new value", newValue, getListValues(newContainerVersion, StringGraphFieldListImpl.class, FIELD_NAME));
			container = newContainerVersion;
		}
	}

	@Test
	@Override
	public void testUpdateSetNull() {
		StringFieldListImpl list = new StringFieldListImpl();
		list.add("A");
		list.add("B");
		NodeResponse firstResponse = updateNode(FIELD_NAME, list);
		String oldVersion = firstResponse.getVersion().getNumber();

		NodeResponse secondResponse = updateNode(FIELD_NAME, null);
		assertThat(secondResponse.getFields().getStringFieldList(FIELD_NAME)).as("Updated Field").isNull();
		assertThat(oldVersion).as("Version should be updated").isNotEqualTo(secondResponse.getVersion().getNumber());

		NodeResponse thirdResponse = updateNode(FIELD_NAME, null);
		assertEquals("The field does not change and thus the version should not be bumped.", thirdResponse.getVersion().getNumber(),
				secondResponse.getVersion().getNumber());

	}

	@Test
	@Override
	public void testUpdateSetEmpty() {
		StringFieldListImpl list = new StringFieldListImpl();
		list.add("A");
		list.add("B");
		NodeResponse firstResponse = updateNode(FIELD_NAME, list);
		String oldVersion = firstResponse.getVersion().getNumber();

		StringFieldListImpl emptyField = new StringFieldListImpl();
		NodeResponse secondResponse = updateNode(FIELD_NAME, emptyField);
		assertThat(secondResponse.getFields().getStringFieldList(FIELD_NAME)).as("Updated field list").isNotNull();
		assertThat(secondResponse.getFields().getStringFieldList(FIELD_NAME).getItems()).as("Field value should be truncated").isEmpty();
		assertThat(secondResponse.getVersion().getNumber()).as("New version number should be generated").isNotEqualTo(oldVersion);

		NodeResponse thirdResponse = updateNode(FIELD_NAME, emptyField);
		assertEquals("The field does not change and thus the version should not be bumped.", thirdResponse.getVersion().getNumber(),
				secondResponse.getVersion().getNumber());
		assertThat(secondResponse.getVersion().getNumber()).as("No new version number should be generated")
				.isEqualTo(secondResponse.getVersion().getNumber());

	}

}
