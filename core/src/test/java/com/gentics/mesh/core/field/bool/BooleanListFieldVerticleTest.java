package com.gentics.mesh.core.field.bool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.list.impl.BooleanGraphFieldListImpl;
import com.gentics.mesh.core.field.AbstractGraphListFieldVerticleTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.list.impl.BooleanFieldListImpl;

public class BooleanListFieldVerticleTest extends AbstractGraphListFieldVerticleTest {

	@Override
	public String getListFieldType() {
		return "boolean";
	}

	@Test
	public void testBooleanList() throws IOException {
		BooleanFieldListImpl listField = new BooleanFieldListImpl();
		listField.add(true);
		listField.add(false);
		listField.add(null);

		NodeResponse response = createNode(FIELD_NAME, listField);
		BooleanFieldListImpl listFromResponse = response.getFields().getBooleanFieldList(FIELD_NAME);
		assertEquals("Only valid values (true,false) should be stored.", 2, listFromResponse.getItems().size());
	}

	@Test
	public void testUpdateNodeWithBooleanField() throws IOException {
		Node node = folder("2015");

		List<List<Boolean>> valueCombinations = Arrays.asList(Arrays.asList(true, false, false), Arrays.asList(false, false, true),
				Collections.emptyList(), Arrays.asList(true, false), Arrays.asList(false));

		for (int i = 0; i < 20; i++) {
			NodeGraphFieldContainer container = node.getGraphFieldContainer("en");
			List<Boolean> oldValue = getListValues(container, BooleanGraphFieldListImpl.class, FIELD_NAME);
			List<Boolean> newValue = valueCombinations.get(i % valueCombinations.size());

			BooleanFieldListImpl list = new BooleanFieldListImpl();
			for (Boolean value : newValue) {
				list.add(value);
			}
			NodeResponse response = updateNode(FIELD_NAME, list);
			BooleanFieldListImpl field = response.getFields().getBooleanFieldList(FIELD_NAME);
			assertThat(field.getItems()).as("Updated field").containsExactlyElementsOf(list.getItems());
			node.reload();
			container.reload();

			assertEquals("Check version number", container.getVersion().nextDraft().toString(), response.getVersion().getNumber());
			assertEquals("Check old value", oldValue, getListValues(container, BooleanGraphFieldListImpl.class, FIELD_NAME));
		}
	}

	@Test
	@Override
	public void testUpdateSetNull() {
		BooleanFieldListImpl list = new BooleanFieldListImpl();
		list.add(true);
		list.add(false);
		NodeResponse firstResponse = updateNode(FIELD_NAME, list);
		String oldVersion = firstResponse.getVersion().getNumber();

		NodeResponse secondResponse = updateNode(FIELD_NAME, null);
		assertThat(secondResponse.getFields().getBooleanFieldList(FIELD_NAME)).as("Updated Field").isNull();
		assertThat(oldVersion).as("Version should be updated").isNotEqualTo(secondResponse.getVersion().getNumber());

		NodeResponse thirdResponse = updateNode(FIELD_NAME, null);
		assertEquals("The field does not change and thus the version should not be bumped.", thirdResponse.getVersion().getNumber(),
				secondResponse.getVersion().getNumber());

	}

	@Test
	@Override
	public void testUpdateSetEmpty() {
		BooleanFieldListImpl list = new BooleanFieldListImpl();
		list.add(true);
		list.add(false);
		NodeResponse firstResponse = updateNode(FIELD_NAME, list);
		String oldVersion = firstResponse.getVersion().getNumber();

		BooleanFieldListImpl emptyField = new BooleanFieldListImpl();
		NodeResponse secondResponse = updateNode(FIELD_NAME, emptyField);
		assertThat(secondResponse.getFields().getBooleanFieldList(FIELD_NAME)).as("Updated field list").isNotNull();
		assertThat(secondResponse.getFields().getBooleanFieldList(FIELD_NAME).getItems()).as("Field value should be truncated").isEmpty();
		assertThat(secondResponse.getVersion().getNumber()).as("New version number should be generated").isNotEqualTo(oldVersion);

		NodeResponse thirdResponse = updateNode(FIELD_NAME, emptyField);
		assertEquals("The field does not change and thus the version should not be bumped.", thirdResponse.getVersion().getNumber(),
				secondResponse.getVersion().getNumber());
		assertThat(secondResponse.getVersion().getNumber()).as("No new version number should be generated")
				.isEqualTo(secondResponse.getVersion().getNumber());
	}
}
