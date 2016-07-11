package com.gentics.mesh.core.field.date;

import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.list.impl.DateGraphFieldListImpl;
import com.gentics.mesh.core.field.AbstractListFieldVerticleTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;

public class DateFieldListVerticleTest extends AbstractListFieldVerticleTest {

	@Override
	public String getListFieldType() {
		return "date";
	}

	@Test
	@Override
	public void testCreateNodeWithField() {
		DateFieldListImpl listField = new DateFieldListImpl();
		listField.add(42L);
		listField.add(41L);

		NodeResponse response = createNode(FIELD_NAME, listField);
		DateFieldListImpl field = response.getFields().getDateFieldList(FIELD_NAME);
		assertThat(field.getItems()).as("List with valid values").containsExactly(42L, 41L);
	}

	@Test
	@Override
	public void testNullValueInListOnCreate() {
		DateFieldListImpl listField = new DateFieldListImpl();
		listField.add(42L);
		listField.add(41L);
		listField.add(null);
		createNodeAndExpectFailure(FIELD_NAME, listField, BAD_REQUEST, "field_list_error_null_not_allowed", FIELD_NAME);
	}

	@Test
	@Override
	public void testNullValueInListOnUpdate() {
		DateFieldListImpl listField = new DateFieldListImpl();
		listField.add(42L);
		listField.add(41L);
		listField.add(null);
		updateNodeFailure(FIELD_NAME, listField, BAD_REQUEST, "field_list_error_null_not_allowed", FIELD_NAME);
	}

	@Test
	@Override
	public void testCreateNodeWithNoField() {
		NodeResponse response = createNode(FIELD_NAME, (Field) null);
		assertThat(response.getFields().getDateFieldList(FIELD_NAME)).as("List field in response should be null").isNull();
	}

	@Test
	@Override
	public void testUpdateSameValue() {
		DateFieldListImpl listField = new DateFieldListImpl();
		listField.add(42L);
		listField.add(41L);

		NodeResponse firstResponse = updateNode(FIELD_NAME, listField);
		String oldVersion = firstResponse.getVersion().getNumber();

		NodeResponse secondResponse = updateNode(FIELD_NAME, listField);
		assertThat(secondResponse.getVersion().getNumber()).as("New version number").isEqualTo(oldVersion);
	}

	@Test
	@Override
	public void testReadNodeWithExistingField() {
		// 1. Update an existing node
		DateFieldListImpl listField = new DateFieldListImpl();
		listField.add(42L);
		listField.add(41L);
		NodeResponse firstResponse = updateNode(FIELD_NAME, listField);

		//2. Read the node
		NodeResponse response = readNode(PROJECT_NAME, firstResponse.getUuid());
		DateFieldListImpl deserializedField = response.getFields().getDateFieldList(FIELD_NAME);
		assertNotNull(deserializedField);
		assertThat(deserializedField.getItems()).as("List field values from updated node").containsExactly(42L, 41L);
	}

	@Test
	@Override
	public void testUpdateNodeFieldWithField() throws IOException {
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

		// Assert that the old version was not modified
		Node node = folder("2015");
		NodeGraphFieldContainer latest = node.getLatestDraftFieldContainer(english());
		assertThat(latest.getVersion().toString()).isEqualTo(secondResponse.getVersion().getNumber());
		assertThat(latest.getDateList(FIELD_NAME)).isNull();
		assertThat(latest.getPreviousVersion().getDateList(FIELD_NAME)).isNotNull();
		List<Number> oldValueList = latest.getPreviousVersion().getDateList(FIELD_NAME).getList().stream().map(item -> item.getDate())
				.collect(Collectors.toList());
		assertThat(oldValueList).containsExactly(42L, 41L);

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
