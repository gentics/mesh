package com.gentics.mesh.core.field.string;

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

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.list.impl.StringGraphFieldListImpl;
import com.gentics.mesh.core.field.AbstractListFieldEndpointTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.syncleus.ferma.tx.Tx;

@MeshTestSetting(useElasticsearch = false, testSize = TestSize.PROJECT_AND_NODE, startServer = true)
public class StringFieldListEndpointTest extends AbstractListFieldEndpointTest {

	@Override
	public String getListFieldType() {
		return "string";
	}

	@Test
	@Override
	public void testCreateNodeWithField() {
		try (Tx tx = tx()) {
			NodeResponse response = createNodeWithField();
			StringFieldListImpl field = response.getFields().getStringFieldList(FIELD_NAME);
			assertThat(field.getItems()).as("Only valid values should be stored").containsExactly("A", "B");
		}
	}

	@Test
	@Override
	public void testNullValueInListOnCreate() {
		try (Tx tx = tx()) {
			StringFieldListImpl listField = new StringFieldListImpl();
			listField.add("A");
			listField.add("B");
			listField.add(null);
			createNodeAndExpectFailure(FIELD_NAME, listField, BAD_REQUEST, "field_list_error_null_not_allowed", FIELD_NAME);
		}
	}

	@Test
	@Override
	public void testNullValueInListOnUpdate() {
		try (Tx tx = tx()) {
			StringFieldListImpl listField = new StringFieldListImpl();
			listField.add("A");
			listField.add("B");
			listField.add(null);
			updateNodeFailure(FIELD_NAME, listField, BAD_REQUEST, "field_list_error_null_not_allowed", FIELD_NAME);
		}
	}

	@Test
	@Override
	public void testCreateNodeWithNoField() {
		try (Tx tx = tx()) {
			NodeResponse response = createNode(FIELD_NAME, (Field) null);
			assertThat(response.getFields().getStringFieldList(FIELD_NAME)).as("List field in reponse should be null").isNull();
		}
	}

	@Test
	@Override
	public void testUpdateSameValue() {
		try (Tx tx = tx()) {
			StringFieldListImpl listField = new StringFieldListImpl();
			listField.add("A");
			listField.add("B");

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
			StringFieldListImpl listField = new StringFieldListImpl();
			listField.add("A");
			listField.add("B");
			NodeResponse firstResponse = updateNode(FIELD_NAME, listField);

			// 2. Read the node
			NodeResponse response = readNode(PROJECT_NAME, firstResponse.getUuid());
			StringFieldListImpl deserializedField = response.getFields().getStringFieldList(FIELD_NAME);
			assertNotNull(deserializedField);
			assertThat(deserializedField.getItems()).as("List field values from updated node").containsExactly("A", "B");
		}
	}

	@Test
	public void testCreateNodeWithNullFieldValue() throws IOException {
		try (Tx tx = tx()) {
			NodeResponse response = createNode(FIELD_NAME, (Field) null);
			StringFieldListImpl nodeField = response.getFields().getStringFieldList(FIELD_NAME);
			assertNull("No string field should have been created.", nodeField);
		}
	}

	@Test
	public void testCreateEmptyStringList() throws IOException {
		try (Tx tx = tx()) {
			StringFieldListImpl listField = new StringFieldListImpl();
			NodeResponse response = createNode(FIELD_NAME, listField);
			StringFieldListImpl listFromResponse = response.getFields().getStringFieldList(FIELD_NAME);
			assertEquals(0, listFromResponse.getItems().size());
		}
	}

	@Test
	public void testCreateNullStringList() throws IOException {
		try (Tx tx = tx()) {
			StringFieldListImpl listField = new StringFieldListImpl();
			listField.setItems(null);
			NodeResponse response = createNode(FIELD_NAME, listField);
			StringFieldListImpl listFromResponse = response.getFields().getStringFieldList(FIELD_NAME);
			assertNull("The string list should be null since the request was sending null instead of an array.", listFromResponse);
		}
	}

	@Test
	public void testStringList() throws IOException {
		try (Tx tx = tx()) {
			StringFieldListImpl listField = new StringFieldListImpl();
			listField.add("A");
			listField.add("B");
			listField.add("C");

			NodeResponse response = createNode(FIELD_NAME, listField);
			StringFieldListImpl listFromResponse = response.getFields().getStringFieldList(FIELD_NAME);
			assertEquals(3, listFromResponse.getItems().size());
			assertEquals(Arrays.asList("A", "B", "C").toString(), listFromResponse.getItems().toString());
		}
	}

	@Test
	@Override
	public void testUpdateNodeFieldWithField() throws IOException {
		Node node = folder("2015");

		List<List<String>> valueCombinations = Arrays.asList(Arrays.asList("A", "B", "C"), Arrays.asList("C", "B", "A"), Collections.emptyList(),
			Arrays.asList("X", "Y"), Arrays.asList("C"));

		NodeGraphFieldContainer container = tx(() -> node.getGraphFieldContainer("en"));
		for (int i = 0; i < 20; i++) {
			StringFieldListImpl list = new StringFieldListImpl();
			List<String> oldValue;
			List<String> newValue;
			try (Tx tx = tx()) {
				oldValue = getListValues(container, StringGraphFieldListImpl.class, FIELD_NAME);
				newValue = valueCombinations.get(i % valueCombinations.size());

				for (String value : newValue) {
					list.add(value);
				}
			}
			NodeResponse response = updateNode(FIELD_NAME, list);
			StringFieldListImpl field = response.getFields().getStringFieldList(FIELD_NAME);
			assertThat(field.getItems()).as("Updated field").containsExactlyElementsOf(list.getItems());

			try (Tx tx = tx()) {
				NodeGraphFieldContainer newContainerVersion = container.getNextVersions().iterator().next();
				assertEquals("The old container version did not match", container.getVersion().nextDraft().toString(),
					response.getVersion().toString());
				assertEquals("Check version number", newContainerVersion.getVersion().toString(), response.getVersion());
				assertEquals("Check old value", oldValue, getListValues(container, StringGraphFieldListImpl.class, FIELD_NAME));
				assertEquals("Check new value", newValue, getListValues(newContainerVersion, StringGraphFieldListImpl.class, FIELD_NAME));
				container = newContainerVersion;
			}
		}
	}

	@Test
	@Override
	public void testUpdateSetNull() {
		StringFieldListImpl list = new StringFieldListImpl();
		list.add("A");
		list.add("B");
		NodeResponse firstResponse = updateNode(FIELD_NAME, list);
		String oldVersion = firstResponse.getVersion();

		NodeResponse secondResponse = updateNode(FIELD_NAME, null);
		assertThat(secondResponse.getFields().getStringFieldList(FIELD_NAME)).as("Updated Field").isNull();
		assertThat(oldVersion).as("Version should be updated").isNotEqualTo(secondResponse.getVersion());

		// Assert that the old version was not modified
		try (Tx tx = tx()) {
			Node node = folder("2015");
			NodeGraphFieldContainer latest = node.getLatestDraftFieldContainer(english());
			assertThat(latest.getVersion().toString()).isEqualTo(secondResponse.getVersion());
			assertThat(latest.getStringList(FIELD_NAME)).isNull();
			assertThat(latest.getPreviousVersion().getStringList(FIELD_NAME)).isNotNull();
			List<String> oldValueList = latest.getPreviousVersion().getStringList(FIELD_NAME).getList().stream().map(item -> item.getString())
				.collect(Collectors.toList());
			assertThat(oldValueList).containsExactly("A", "B");

			NodeResponse thirdResponse = updateNode(FIELD_NAME, null);
			assertEquals("The field does not change and thus the version should not be bumped.", thirdResponse.getVersion(),
				secondResponse.getVersion());
		}
	}

	@Test
	@Override
	public void testUpdateSetEmpty() {
		StringFieldListImpl list = new StringFieldListImpl();
		list.add("A");
		list.add("B");
		NodeResponse firstResponse = updateNode(FIELD_NAME, list);
		String oldVersion = firstResponse.getVersion();

		StringFieldListImpl emptyField = new StringFieldListImpl();
		NodeResponse secondResponse = updateNode(FIELD_NAME, emptyField);
		assertThat(secondResponse.getFields().getStringFieldList(FIELD_NAME)).as("Updated field list").isNotNull();
		assertThat(secondResponse.getFields().getStringFieldList(FIELD_NAME).getItems()).as("Field value should be truncated").isEmpty();
		assertThat(secondResponse.getVersion()).as("New version number should be generated").isNotEqualTo(oldVersion);

		NodeResponse thirdResponse = updateNode(FIELD_NAME, emptyField);
		assertEquals("The field does not change and thus the version should not be bumped.", thirdResponse.getVersion(), secondResponse.getVersion());
		assertThat(secondResponse.getVersion()).as("No new version number should be generated").isEqualTo(secondResponse.getVersion());
	}

	@Override
	public NodeResponse createNodeWithField() {
		StringFieldListImpl listField = new StringFieldListImpl();
		listField.add("A");
		listField.add("B");

		return createNode(FIELD_NAME, listField);
	}
}
