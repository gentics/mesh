package com.gentics.mesh.core.field.json;

import static com.gentics.mesh.core.field.json.JsonFieldTestHelper.make;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.field.AbstractListFieldEndpointTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.list.impl.JsonFieldListImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;

import io.vertx.core.json.JsonObject;

@MeshTestSetting(testSize = TestSize.PROJECT_AND_NODE, startServer = true)
public class JsonFieldListEndpointTest extends AbstractListFieldEndpointTest {

	protected static final List<JsonObject> TEST_LIST = List.of(make("A"), make("B"), make("C"));

	@Override
	public String getListFieldType() {
		return "json";
	}

	@Test
	@Override
	public void testCreateNodeWithField() {
		NodeResponse response = createNodeWithField();
		JsonFieldListImpl field = response.getFields().getJsonFieldList(FIELD_NAME);
		assertThat(field.getItems()).as("Only valid values should be stored").containsExactlyElementsOf(TEST_LIST);
	}

	@Test
	@Override
	public void testNullValueInListOnCreate() {
		JsonFieldListImpl listField = new JsonFieldListImpl();
		listField.add(make("A"));
		listField.add(make("B"));
		listField.add(null);
		createNodeAndExpectFailure(FIELD_NAME, listField, BAD_REQUEST, "field_list_error_null_not_allowed", FIELD_NAME);
	}

	@Test
	@Override
	public void testNullValueInListOnUpdate() {
		JsonFieldListImpl listField = new JsonFieldListImpl();
		listField.add(make("A"));
		listField.add(make("B"));
		listField.add(null);
		updateNodeFailure(FIELD_NAME, listField, BAD_REQUEST, "field_list_error_null_not_allowed", FIELD_NAME);
	}

	@Test
	@Override
	public void testCreateNodeWithNoField() {
		NodeResponse response = createNode(FIELD_NAME, (Field) null);
		assertThat(response.getFields().getJsonFieldList(FIELD_NAME)).as("List field in reponse should be null").isNull();
	}

	@Test
	@Override
	public void testUpdateSameValue() {
		JsonFieldListImpl listField = new JsonFieldListImpl();
		listField.setItems(TEST_LIST);

		NodeResponse firstResponse = updateNode(FIELD_NAME, listField);
		String oldVersion = firstResponse.getVersion();

		NodeResponse secondResponse = updateNode(FIELD_NAME, listField);
		assertThat(secondResponse.getVersion()).as("New version number").isEqualTo(oldVersion);
	}

	@Test
	@Override
	public void testReadNodeWithExistingField() {
		// 1. Update an existing node
		JsonFieldListImpl listField = new JsonFieldListImpl();
		listField.setItems(TEST_LIST);
		NodeResponse firstResponse = updateNode(FIELD_NAME, listField);

		// 2. Read the node
		NodeResponse response = readNode(PROJECT_NAME, firstResponse.getUuid());
		JsonFieldListImpl deserializedField = response.getFields().getJsonFieldList(FIELD_NAME);
		assertNotNull(deserializedField);
		assertThat(deserializedField.getItems()).as("List field values from updated node").containsExactlyElementsOf(TEST_LIST);
	}

	@Test
	public void testCreateNodeWithNullFieldValue() throws IOException {
		NodeResponse response = createNode(FIELD_NAME, (Field) null);
		JsonFieldListImpl nodeField = response.getFields().getJsonFieldList(FIELD_NAME);
		assertNull("No json field should have been created.", nodeField);
	}

	@Test
	public void testCreateEmptyStringList() throws IOException {
		JsonFieldListImpl listField = new JsonFieldListImpl();
		NodeResponse response = createNode(FIELD_NAME, listField);
		JsonFieldListImpl listFromResponse = response.getFields().getJsonFieldList(FIELD_NAME);
		assertEquals(0, listFromResponse.getItems().size());
	}

	@Test
	public void testCreateNullStringList() throws IOException {
		JsonFieldListImpl listField = new JsonFieldListImpl();
		listField.setItems(null);
		NodeResponse response = createNode(FIELD_NAME, listField);
		JsonFieldListImpl listFromResponse = response.getFields().getJsonFieldList(FIELD_NAME);
		assertNull("The json list should be null since the request was sending null instead of an array.", listFromResponse);
	}

	@Test
	public void testJsonList() throws IOException {
		JsonFieldListImpl listField = new JsonFieldListImpl();
		listField.setItems(TEST_LIST);

		NodeResponse response = createNode(FIELD_NAME, listField);
		JsonFieldListImpl listFromResponse = response.getFields().getJsonFieldList(FIELD_NAME);
		assertEquals(3, listFromResponse.getItems().size());
		assertEquals(TEST_LIST.toString(), listFromResponse.getItems().toString());
	}

	@Test
	@Override
	public void testUpdateNodeFieldWithField() throws IOException {
		disableAutoPurge();
		HibNode node = folder("2015");

		List<List<String>> valueCombinations = Arrays.asList(Arrays.asList("A", "B", "C"), Arrays.asList("C", "B", "A"), Collections.emptyList(),
			Arrays.asList("X", "Y"), Arrays.asList("C"));

		HibNodeFieldContainer container = tx(tx -> { return tx.contentDao().getFieldContainer(node, "en"); });
		for (int i = 0; i < 20; i++) {
			JsonFieldListImpl list = new JsonFieldListImpl();
			List<JsonObject> oldValue;
			List<JsonObject> newValue;
			try (Tx tx = tx()) {
				oldValue = getListValues(container::getJsonList, FIELD_NAME);
				newValue = valueCombinations.get(i % valueCombinations.size()).stream().map(JsonFieldTestHelper::make).collect(Collectors.toList());

				for (JsonObject value : newValue) {
					list.add(value);
				}
			}
			NodeResponse response = updateNode(FIELD_NAME, list);
			JsonFieldListImpl field = response.getFields().getJsonFieldList(FIELD_NAME);
			assertThat(field.getItems()).as("Updated field").containsExactlyElementsOf(list.getItems());

			try (Tx tx = tx()) {
				ContentDao contentDao = tx.contentDao();
				HibNodeFieldContainer newContainerVersion = contentDao.getNextVersions(container).iterator().next();
				assertEquals("The old container version did not match", container.getVersion().nextDraft().toString(),
					response.getVersion().toString());
				assertEquals("Check version number", newContainerVersion.getVersion().toString(), response.getVersion());
				assertEquals("Check old value", oldValue, getListValues(container::getJsonList, FIELD_NAME));
				assertEquals("Check new value", newValue, getListValues(newContainerVersion::getJsonList, FIELD_NAME));
				container = newContainerVersion;
			}
		}
	}

	@Test
	@Override
	public void testUpdateSetNull() {
		disableAutoPurge();

		JsonFieldListImpl list = new JsonFieldListImpl();
		list.setItems(TEST_LIST);
		NodeResponse firstResponse = updateNode(FIELD_NAME, list);
		String oldVersion = firstResponse.getVersion();

		NodeResponse secondResponse = updateNode(FIELD_NAME, null);
		assertThat(secondResponse.getFields().getJsonFieldList(FIELD_NAME)).as("Updated Field").isNull();
		assertThat(oldVersion).as("Version should be updated").isNotEqualTo(secondResponse.getVersion());

		// Assert that the old version was not modified
		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();
			HibNode node = folder("2015");
			HibNodeFieldContainer latest = contentDao.getLatestDraftFieldContainer(node, english());
			assertThat(latest.getVersion().toString()).isEqualTo(secondResponse.getVersion());
			assertThat(latest.getJsonList(FIELD_NAME)).isNull();
			assertThat(latest.getPreviousVersion().getJsonList(FIELD_NAME)).isNotNull();
			List<JsonObject> oldValueList = latest.getPreviousVersion().getJsonList(FIELD_NAME).getList().stream().map(item -> item.getJson())
				.collect(Collectors.toList());
			assertThat(oldValueList).containsExactlyElementsOf(TEST_LIST);
		}
		NodeResponse thirdResponse = updateNode(FIELD_NAME, null);
		assertEquals("The field does not change and thus the version should not be bumped.", thirdResponse.getVersion(),
			secondResponse.getVersion());
	}

	@Test
	@Override
	public void testUpdateSetEmpty() {
		JsonFieldListImpl list = new JsonFieldListImpl();
		list.setItems(TEST_LIST);
		NodeResponse firstResponse = updateNode(FIELD_NAME, list);
		String oldVersion = firstResponse.getVersion();

		JsonFieldListImpl emptyField = new JsonFieldListImpl();
		NodeResponse secondResponse = updateNode(FIELD_NAME, emptyField);
		assertThat(secondResponse.getFields().getJsonFieldList(FIELD_NAME)).as("Updated field list").isNotNull();
		assertThat(secondResponse.getFields().getJsonFieldList(FIELD_NAME).getItems()).as("Field value should be truncated").isEmpty();
		assertThat(secondResponse.getVersion()).as("New version number should be generated").isNotEqualTo(oldVersion);

		NodeResponse thirdResponse = updateNode(FIELD_NAME, emptyField);
		assertEquals("The field does not change and thus the version should not be bumped.", thirdResponse.getVersion(), secondResponse.getVersion());
		assertThat(secondResponse.getVersion()).as("No new version number should be generated").isEqualTo(secondResponse.getVersion());
	}

	@Override
	public NodeResponse createNodeWithField() {
		JsonFieldListImpl listField = new JsonFieldListImpl();
		listField.setItems(TEST_LIST);

		return createNode(FIELD_NAME, listField);
	}
}
