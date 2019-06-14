package com.gentics.mesh.core.field.bool;

import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
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

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.list.impl.BooleanGraphFieldListImpl;
import com.gentics.mesh.core.field.AbstractListFieldEndpointTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.list.impl.BooleanFieldListImpl;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = TestSize.PROJECT_AND_NODE, startServer = true)
public class BooleanListFieldEndpointTest extends AbstractListFieldEndpointTest {

	@Override
	public String getListFieldType() {
		return "boolean";
	}

	@Test
	@Override
	public void testCreateNodeWithField() {
		try (Tx tx = tx()) {
			NodeResponse response = createNodeWithField();
			BooleanFieldListImpl field = response.getFields().getBooleanFieldList(FIELD_NAME);
			assertThat(field.getItems()).as("Only valid values (true,false) should be stored").containsExactly(true, false);
		}
	}

	@Test
	@Override
	public void testNullValueInListOnCreate() {
		try (Tx tx = tx()) {
			BooleanFieldListImpl listField = new BooleanFieldListImpl();
			listField.add(true);
			listField.add(false);
			listField.add(null);
			createNodeAndExpectFailure(FIELD_NAME, listField, BAD_REQUEST, "field_list_error_null_not_allowed", FIELD_NAME);
		}
	}

	@Test
	@Override
	public void testNullValueInListOnUpdate() {
		try (Tx tx = tx()) {
			BooleanFieldListImpl listField = new BooleanFieldListImpl();
			listField.add(true);
			listField.add(false);
			listField.add(null);
			updateNodeFailure(FIELD_NAME, listField, BAD_REQUEST, "field_list_error_null_not_allowed", FIELD_NAME);
		}
	}

	@Test
	@Override
	public void testCreateNodeWithNoField() {
		try (Tx tx = tx()) {
			NodeResponse response = createNode(FIELD_NAME, (Field) null);
			assertThat(response.getFields().getBooleanFieldList(FIELD_NAME)).as("List field in response should be null").isNull();
		}
	}

	@Test
	@Override
	public void testUpdateSameValue() {
		try (Tx tx = tx()) {
			BooleanFieldListImpl listField = new BooleanFieldListImpl();
			listField.add(true);
			listField.add(false);

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
			BooleanFieldListImpl listField = new BooleanFieldListImpl();
			listField.add(true);
			listField.add(false);
			NodeResponse firstResponse = updateNode(FIELD_NAME, listField);

			// 2. Read the node
			NodeResponse response = readNode(PROJECT_NAME, firstResponse.getUuid());
			BooleanFieldListImpl deserializedBooleanField = response.getFields().getBooleanFieldList(FIELD_NAME);
			assertNotNull(deserializedBooleanField);
			assertThat(deserializedBooleanField.getItems()).as("Only valid list field values should be listed").containsExactly(true, false);
		}
	}

	@Test
	@Override
	public void testUpdateNodeFieldWithField() throws IOException {
		disableAutoPurge();

		Node node = folder("2015");

		List<List<Boolean>> valueCombinations = Arrays.asList(Arrays.asList(true, false, false), Arrays.asList(false, false, true),
			Collections.emptyList(), Arrays.asList(true, false), Arrays.asList(false));

		for (int i = 0; i < 20; i++) {
			BooleanFieldListImpl list = new BooleanFieldListImpl();
			NodeGraphFieldContainer container;
			List<Boolean> oldValue;
			List<Boolean> newValue;

			try (Tx tx = tx()) {
				container = node.getGraphFieldContainer("en");
				oldValue = getListValues(container, BooleanGraphFieldListImpl.class, FIELD_NAME);
				newValue = valueCombinations.get(i % valueCombinations.size());

				for (Boolean value : newValue) {
					list.add(value);
				}
			}
			NodeResponse response = updateNode(FIELD_NAME, list);
			BooleanFieldListImpl field = response.getFields().getBooleanFieldList(FIELD_NAME);
			assertThat(field.getItems()).as("Updated field").containsExactlyElementsOf(list.getItems());

			try (Tx tx = tx()) {
				assertEquals("Check version number", container.getVersion().nextDraft().toString(), response.getVersion());
				assertEquals("Check old value", oldValue, getListValues(container, BooleanGraphFieldListImpl.class, FIELD_NAME));
			}
		}
	}

	@Test
	@Override
	public void testUpdateSetNull() {
		disableAutoPurge();

		BooleanFieldListImpl list = new BooleanFieldListImpl();
		list.add(true);
		list.add(false);
		NodeResponse firstResponse = updateNode(FIELD_NAME, list);
		String oldVersion = firstResponse.getVersion();

		NodeResponse secondResponse = updateNode(FIELD_NAME, null);
		assertThat(secondResponse.getFields().getBooleanFieldList(FIELD_NAME)).as("Updated Field").isNull();
		assertThat(oldVersion).as("Version should be updated").isNotEqualTo(secondResponse.getVersion());

		// Assert that the old version was not modified
		try (Tx tx = tx()) {
			Node node = folder("2015");
			NodeGraphFieldContainer latest = node.getLatestDraftFieldContainer(english());
			assertThat(latest.getVersion().toString()).isEqualTo(secondResponse.getVersion());
			assertThat(latest.getBooleanList(FIELD_NAME)).isNull();
			assertThat(latest.getPreviousVersion().getBooleanList(FIELD_NAME)).isNotNull();
			List<Boolean> oldValueList = latest.getPreviousVersion().getBooleanList(FIELD_NAME).getList().stream().map(item -> item.getBoolean())
				.collect(Collectors.toList());
			assertThat(oldValueList).containsExactly(true, false);

			NodeResponse thirdResponse = updateNode(FIELD_NAME, null);
			assertEquals("The field does not change and thus the version should not be bumped.", thirdResponse.getVersion(),
				secondResponse.getVersion());
		}
	}

	@Test
	@Override
	public void testUpdateSetEmpty() {
		BooleanFieldListImpl list = new BooleanFieldListImpl();
		list.add(true);
		list.add(false);
		NodeResponse firstResponse = updateNode(FIELD_NAME, list);
		String oldVersion = firstResponse.getVersion();

		BooleanFieldListImpl emptyField = new BooleanFieldListImpl();
		NodeResponse secondResponse = updateNode(FIELD_NAME, emptyField);
		assertThat(secondResponse.getFields().getBooleanFieldList(FIELD_NAME)).as("Updated field list").isNotNull();
		assertThat(secondResponse.getFields().getBooleanFieldList(FIELD_NAME).getItems()).as("Field value should be truncated").isEmpty();
		assertThat(secondResponse.getVersion()).as("New version number should be generated").isNotEqualTo(oldVersion);

		NodeResponse thirdResponse = updateNode(FIELD_NAME, emptyField);
		assertEquals("The field does not change and thus the version should not be bumped.", thirdResponse.getVersion(), secondResponse.getVersion());
		assertThat(secondResponse.getVersion()).as("No new version number should be generated").isEqualTo(secondResponse.getVersion());
	}

	@Override
	public NodeResponse createNodeWithField() {
		BooleanFieldListImpl listField = new BooleanFieldListImpl();
		listField.add(true);
		listField.add(false);

		return createNode(FIELD_NAME, listField);
	}
}
