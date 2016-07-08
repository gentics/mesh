package com.gentics.mesh.core.field.date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.list.impl.DateGraphFieldListImpl;
import com.gentics.mesh.core.field.AbstractGraphListFieldVerticleTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;

public class DateFieldListVerticleTest extends AbstractGraphListFieldVerticleTest {

	@Override
	public String getListFieldType() {
		return "date";
	}

	@Test
	public void testDateList() throws IOException {
		DateFieldListImpl listField = new DateFieldListImpl();
		listField.add((System.currentTimeMillis() / 1000) + 1);
		listField.add((System.currentTimeMillis() / 1000) + 2);
		listField.add((System.currentTimeMillis() / 1000) + 3);

		NodeResponse response = createNode(FIELD_NAME, listField);
		DateFieldListImpl listFromResponse = response.getFields().getDateFieldList(FIELD_NAME);
		assertEquals(3, listFromResponse.getItems().size());
	}

	@Test
	public void testUpdateNodeWithDateField() throws IOException {
		Node node = folder("2015");

		List<List<Long>> valueCombinations = Arrays.asList(Arrays.asList(1L, 2L, 3L), Arrays.asList(3L, 2L, 1L), Collections.emptyList(),
				Arrays.asList(4711L, 815L), Arrays.asList(3L));

		for (int i = 0; i < 20; i++) {
			NodeGraphFieldContainer container = node.getGraphFieldContainer("en");
			List<Long> oldValue = getListValues(container, DateGraphFieldListImpl.class, FIELD_NAME);
			List<Long> newValue = valueCombinations.get(i % valueCombinations.size());

			DateFieldListImpl list = new DateFieldListImpl();
			for (Long value : newValue) {
				list.add(value);
			}
			NodeResponse response = updateNode(FIELD_NAME, list);
			DateFieldListImpl field = response.getFields().getDateFieldList(FIELD_NAME);
			assertThat(field.getItems()).as("Updated field").containsExactlyElementsOf(list.getItems());
			node.reload();
			container.reload();

			assertEquals("Check version number", container.getVersion().nextDraft().toString(), response.getVersion().getNumber());
			assertEquals("Check old value", oldValue, getListValues(container, DateGraphFieldListImpl.class, FIELD_NAME));
		}
	}

	@Test
	@Override
	public void testUpdateSetNull() {
		DateFieldListImpl list = new DateFieldListImpl();
		list.add(42L);
		list.add(41L);
		NodeResponse firstResponse = updateNode(FIELD_NAME, list);
		String oldVersion = firstResponse.getVersion().getNumber();

		NodeResponse secondResponse = updateNode(FIELD_NAME, null);
		assertThat(secondResponse.getFields().getDateFieldList(FIELD_NAME)).as("Updated Field").isNull();
		assertThat(oldVersion).as("Version should be updated").isNotEqualTo(secondResponse.getVersion().getNumber());

		NodeResponse thirdResponse = updateNode(FIELD_NAME, null);
		assertEquals("The field does not change and thus the version should not be bumped.", thirdResponse.getVersion().getNumber(),
				secondResponse.getVersion().getNumber());

	}

	@Test
	@Override
	public void testUpdateSetEmpty() {
		DateFieldListImpl list = new DateFieldListImpl();
		list.add(42L);
		list.add(41L);
		NodeResponse firstResponse = updateNode(FIELD_NAME, list);
		String oldVersion = firstResponse.getVersion().getNumber();

		DateFieldListImpl emptyField = new DateFieldListImpl();
		NodeResponse secondResponse = updateNode(FIELD_NAME, emptyField);
		assertThat(secondResponse.getFields().getDateFieldList(FIELD_NAME)).as("Updated field list").isNotNull();
		assertThat(secondResponse.getFields().getDateFieldList(FIELD_NAME).getItems()).as("Field value should be truncated").isEmpty();
		assertThat(secondResponse.getVersion().getNumber()).as("New version number should be generated").isNotEqualTo(oldVersion);

		NodeResponse thirdResponse = updateNode(FIELD_NAME, emptyField);
		assertEquals("The field does not change and thus the version should not be bumped.", thirdResponse.getVersion().getNumber(),
				secondResponse.getVersion().getNumber());
		assertThat(secondResponse.getVersion().getNumber()).as("No new version number should be generated")
				.isEqualTo(secondResponse.getVersion().getNumber());

	}

}
