package com.gentics.mesh.core.field.date;

import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.DateUtils.toISO8601;
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
import com.gentics.mesh.core.data.dao.ContentDaoWrapper;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.list.impl.DateGraphFieldListImpl;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.field.AbstractListFieldEndpointTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;

@MeshTestSetting(testSize = TestSize.PROJECT_AND_NODE, startServer = true)
public class DateFieldListEndpointTest extends AbstractListFieldEndpointTest {

	@Override
	public String getListFieldType() {
		return "date";
	}

	@Test
	@Override
	public void testCreateNodeWithField() {
		try (Tx tx = tx()) {
			DateFieldListImpl listField = new DateFieldListImpl();
			String dateA = toISO8601(4200L);
			String dateB = toISO8601(4100L);
			listField.add(dateA);
			listField.add(dateB);

			NodeResponse response = createNode(FIELD_NAME, listField);
			DateFieldListImpl field = response.getFields().getDateFieldList(FIELD_NAME);
			assertThat(field.getItems()).as("List with valid values").containsExactly(dateA, dateB);
		}
	}

	@Test
	@Override
	public void testNullValueInListOnCreate() {
		try (Tx tx = tx()) {
			DateFieldListImpl listField = new DateFieldListImpl();
			listField.add(toISO8601(4200L));
			listField.add(toISO8601(4100L));
			listField.add(null);
			createNodeAndExpectFailure(FIELD_NAME, listField, BAD_REQUEST, "field_list_error_null_not_allowed", FIELD_NAME);
		}
	}

	@Test
	@Override
	public void testNullValueInListOnUpdate() {
		try (Tx tx = tx()) {
			DateFieldListImpl listField = new DateFieldListImpl();
			String dateA = toISO8601(4200L);
			String dateB = toISO8601(4100L);
			listField.add(dateA);
			listField.add(dateB);
			listField.add(null);
			updateNodeFailure(FIELD_NAME, listField, BAD_REQUEST, "field_list_error_null_not_allowed", FIELD_NAME);
		}
	}

	@Test
	@Override
	public void testCreateNodeWithNoField() {
		try (Tx tx = tx()) {
			NodeResponse response = createNode(FIELD_NAME, (Field) null);
			assertThat(response.getFields().getDateFieldList(FIELD_NAME)).as("List field in response should be null").isNull();
		}
	}

	@Test
	@Override
	public void testUpdateSameValue() {
		try (Tx tx = tx()) {
			DateFieldListImpl listField = new DateFieldListImpl();
			listField.add(toISO8601(4200L));
			listField.add(toISO8601(4100L));

			NodeResponse firstResponse = updateNode(FIELD_NAME, listField);
			String oldVersion = firstResponse.getVersion();

			NodeResponse secondResponse = updateNode(FIELD_NAME, listField);
			assertThat(secondResponse.getVersion()).as("New version number").isEqualTo(oldVersion);
		}
	}

	@Test
	@Override
	public void testReadNodeWithExistingField() {
		try (Tx tx = tx()) {
			// 1. Update an existing node
			DateFieldListImpl listField = new DateFieldListImpl();
			listField.add(toISO8601(4200L));
			listField.add(toISO8601(4100L));
			NodeResponse firstResponse = updateNode(FIELD_NAME, listField);

			// 2. Read the node
			NodeResponse response = readNode(PROJECT_NAME, firstResponse.getUuid());
			DateFieldListImpl deserializedField = response.getFields().getDateFieldList(FIELD_NAME);
			assertNotNull(deserializedField);
			assertThat(deserializedField.getItems()).as("List field values from updated node").containsExactly(toISO8601(4200L), toISO8601(4100L));
		}
	}

	@Test
	@Override
	public void testUpdateNodeFieldWithField() throws IOException {
		disableAutoPurge();

		HibNode node = folder("2015");

		List<List<String>> valueCombinations = Arrays.asList(Arrays.asList(toISO8601(1000L), toISO8601(2000L), toISO8601(3000L)),
			Arrays.asList(toISO8601(3000L), toISO8601(2000L), toISO8601(1000L)), Collections.emptyList(),
			Arrays.asList(toISO8601(471100L), toISO8601(81500L)), Arrays.asList(toISO8601(3000L)));

		for (int i = 0; i < 20; i++) {
			DateFieldListImpl list = new DateFieldListImpl();
			NodeGraphFieldContainer container;
			List<Long> oldValue;
			try (Tx tx = tx()) {
				container = boot().contentDao().getGraphFieldContainer(node, "en");
				oldValue = getListValues(container, DateGraphFieldListImpl.class, FIELD_NAME);
				List<String> newValue = valueCombinations.get(i % valueCombinations.size());
				for (String value : newValue) {
					list.add(value);
				}
			}

			NodeResponse response = updateNode(FIELD_NAME, list);

			try (Tx tx = tx()) {
				DateFieldListImpl field = response.getFields().getDateFieldList(FIELD_NAME);
				assertThat(field.getItems()).as("Updated field").containsExactlyElementsOf(list.getItems());

				assertEquals("Check version number", container.getVersion().nextDraft().toString(), response.getVersion());
				assertEquals("Check old value", oldValue, getListValues(container, DateGraphFieldListImpl.class, FIELD_NAME));
			}
		}
	}

	@Test
	@Override
	public void testUpdateSetNull() {
		disableAutoPurge();

		NodeResponse secondResponse;
		HibNode node = folder("2015");

		DateFieldListImpl list = new DateFieldListImpl();
		String dateA = toISO8601(42000L);
		String dateB = toISO8601(41000L);
		list.add(dateA);
		list.add(dateB);

		NodeResponse firstResponse = updateNode(FIELD_NAME, list);
		String oldVersion = firstResponse.getVersion();

		secondResponse = updateNode(FIELD_NAME, null);
		assertThat(secondResponse.getFields().getDateFieldList(FIELD_NAME)).as("Updated Field").isNull();
		assertThat(oldVersion).as("Version should be updated").isNotEqualTo(secondResponse.getVersion());

		try (Tx tx = tx()) {
			ContentDaoWrapper contentDao = tx.contentDao();
			// Assert that the old version was not modified
			NodeGraphFieldContainer latest = contentDao.getLatestDraftFieldContainer(node, english());
			assertThat(latest.getVersion().toString()).isEqualTo(secondResponse.getVersion());
			assertThat(latest.getDateList(FIELD_NAME)).isNull();
			assertThat(latest.getPreviousVersion().getDateList(FIELD_NAME)).isNotNull();
			List<Number> oldValueList = latest.getPreviousVersion().getDateList(FIELD_NAME).getList().stream().map(item -> item.getDate())
				.collect(Collectors.toList());
			assertThat(oldValueList).containsExactly(42000L, 41000L);

			NodeResponse thirdResponse = updateNode(FIELD_NAME, null);
			assertEquals("The field does not change and thus the version should not be bumped.", thirdResponse.getVersion(),
				secondResponse.getVersion());
		}
	}

	@Test
	@Override
	public void testUpdateSetEmpty() {
		DateFieldListImpl list = new DateFieldListImpl();
		String dateA = toISO8601(4200L);
		String dateB = toISO8601(4100L);
		list.add(dateA);
		list.add(dateB);

		NodeResponse firstResponse = updateNode(FIELD_NAME, list);
		String oldVersion = firstResponse.getVersion();

		DateFieldListImpl emptyField = new DateFieldListImpl();
		NodeResponse secondResponse = updateNode(FIELD_NAME, emptyField);
		assertThat(secondResponse.getFields().getDateFieldList(FIELD_NAME)).as("Updated field list").isNotNull();
		assertThat(secondResponse.getFields().getDateFieldList(FIELD_NAME).getItems()).as("Field value should be truncated").isEmpty();
		assertThat(secondResponse.getVersion()).as("New version number should be generated").isNotEqualTo(oldVersion);

		NodeResponse thirdResponse = updateNode(FIELD_NAME, emptyField);
		assertEquals("The field does not change and thus the version should not be bumped.", thirdResponse.getVersion(), secondResponse.getVersion());
		assertThat(secondResponse.getVersion()).as("No new version number should be generated").isEqualTo(secondResponse.getVersion());
	}

	@Override
	public NodeResponse createNodeWithField() {
		DateFieldListImpl listField = new DateFieldListImpl();
		String dateA = toISO8601(4200L);
		String dateB = toISO8601(4100L);
		listField.add(dateA);
		listField.add(dateB);

		return createNode(FIELD_NAME, listField);
	}

}
