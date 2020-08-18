package com.gentics.mesh.core.field.html;

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

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.dao.ContentDaoWrapper;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.list.impl.HtmlGraphFieldListImpl;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.field.AbstractListFieldEndpointTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.HtmlFieldListImpl;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = TestSize.PROJECT_AND_NODE, startServer = true)
public class HtmlFieldListEndpointTest extends AbstractListFieldEndpointTest {

	@Override
	public String getListFieldType() {
		return "html";
	}

	@Test
	@Override
	public void testCreateNodeWithField() {
		try (Tx tx = tx()) {
			NodeResponse response = createNodeWithField();
			HtmlFieldListImpl field = response.getFields().getHtmlFieldList(FIELD_NAME);
			assertThat(field.getItems()).containsExactly("A", "B", "C");
		}
	}

	@Test
	@Override
	public void testNullValueInListOnCreate() {
		try (Tx tx = tx()) {
			HtmlFieldListImpl listField = new HtmlFieldListImpl();
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
			HtmlFieldListImpl listField = new HtmlFieldListImpl();
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
			assertThat(response.getFields().getHtmlFieldList(FIELD_NAME)).as("List field in reponse should be null").isNull();
		}
	}

	@Test
	@Override
	public void testUpdateSameValue() {
		try (Tx tx = tx()) {
			HtmlFieldListImpl listField = new HtmlFieldListImpl();
			listField.add("A");
			listField.add("B");
			listField.add("C");

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
			HtmlFieldListImpl listField = new HtmlFieldListImpl();
			listField.add("A");
			listField.add("B");
			listField.add("C");
			NodeResponse firstResponse = updateNode(FIELD_NAME, listField);

			// 2. Read the node
			NodeResponse response = readNode(PROJECT_NAME, firstResponse.getUuid());
			HtmlFieldListImpl deserializedField = response.getFields().getHtmlFieldList(FIELD_NAME);
			assertNotNull(deserializedField);
			assertThat(deserializedField.getItems()).as("List field values from updated node").containsExactly("A", "B", "C");
		}
	}

	@Test
	public void testHtmlList() throws IOException {
		try (Tx tx = tx()) {
			HtmlFieldListImpl listField = new HtmlFieldListImpl();
			listField.add("A");
			listField.add("B");
			listField.add("C");

			NodeResponse response = createNode(FIELD_NAME, listField);
			HtmlFieldListImpl listFromResponse = response.getFields().getHtmlFieldList(FIELD_NAME);
			assertEquals(3, listFromResponse.getItems().size());
		}
	}

	@Test
	@Override
	public void testUpdateNodeFieldWithField() throws IOException {
		disableAutoPurge();
		Node node = folder("2015");

		List<List<String>> valueCombinations = Arrays.asList(Arrays.asList("A", "B", "C"), Arrays.asList("C", "B", "A"), Collections.emptyList(),
			Arrays.asList("X", "Y"), Arrays.asList("C"));

		NodeGraphFieldContainer container = tx(() -> boot().contentDao().getGraphFieldContainer(node, "en"));
		for (int i = 0; i < 20; i++) {
			List<String> oldValue;
			List<String> newValue;
			try (Tx tx = tx()) {
				oldValue = getListValues(container, HtmlGraphFieldListImpl.class, FIELD_NAME);
				newValue = valueCombinations.get(i % valueCombinations.size());
			}

			// Prepare update request and update the field
			HtmlFieldListImpl list = new HtmlFieldListImpl();
			for (String value : newValue) {
				list.add(value);
			}
			System.out.println("Update to " + list);
			NodeResponse response = updateNode(FIELD_NAME, list);

			// Assert the update response
			try (Tx tx = tx()) {
				HtmlFieldListImpl field = response.getFields().getHtmlFieldList(FIELD_NAME);
				assertThat(field.getItems()).as("Updated field").containsExactlyElementsOf(list.getItems());

				// Assert the update within the graph
				NodeGraphFieldContainer updatedContainer = container.getNextVersions().iterator().next();
				assertEquals("The container version number did not match up with the response version number.",
					updatedContainer.getVersion().toString(), response.getVersion());
				assertEquals("We expected container {" + container.getVersion().toString() + "} to contain the old value.", newValue,
					getListValues(updatedContainer, HtmlGraphFieldListImpl.class, FIELD_NAME));
				// assertEquals("We expected container {" + updatedContainer.getVersion().toString() +"} to contain the old value.", oldValue,
				// getListValues(updatedContainer, HtmlGraphFieldListImpl.class, FIELD_NAME));
				container = updatedContainer;
			}
		}
	}

	@Test
	@Override
	public void testUpdateSetNull() {
		disableAutoPurge();
		Node node = folder("2015");

		HtmlFieldListImpl list = new HtmlFieldListImpl();
		list.add("A");
		list.add("B");
		updateNode(FIELD_NAME, list);

		NodeResponse secondResponse = updateNode(FIELD_NAME, null);
		assertThat(secondResponse.getFields().getHtmlFieldList(FIELD_NAME)).as("Updated Field").isNull();

		// Assert that the old version was not modified
		try (Tx tx = tx()) {
			ContentDaoWrapper contentDao = tx.data().contentDao();
			NodeGraphFieldContainer latest = contentDao.getLatestDraftFieldContainer(node, english());
			assertThat(latest.getVersion().toString()).isEqualTo(secondResponse.getVersion());
			assertThat(latest.getHTMLList(FIELD_NAME)).isNull();
			assertThat(latest.getPreviousVersion().getHTMLList(FIELD_NAME)).isNotNull();
			List<String> oldValueList = latest.getPreviousVersion().getHTMLList(FIELD_NAME).getList().stream().map(item -> item.getHTML())
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
		HtmlFieldListImpl list = new HtmlFieldListImpl();
		list.add("A");
		list.add("B");
		NodeResponse firstResponse = updateNode(FIELD_NAME, list);
		String oldVersion = firstResponse.getVersion();

		DateFieldListImpl emptyField = new DateFieldListImpl();
		NodeResponse secondResponse = updateNode(FIELD_NAME, emptyField);
		assertThat(secondResponse.getFields().getHtmlFieldList(FIELD_NAME)).as("Updated field list").isNotNull();
		assertThat(secondResponse.getFields().getHtmlFieldList(FIELD_NAME).getItems()).as("Field value should be truncated").isEmpty();
		assertThat(secondResponse.getVersion()).as("New version number should be generated").isNotEqualTo(oldVersion);

		NodeResponse thirdResponse = updateNode(FIELD_NAME, emptyField);
		assertEquals("The field does not change and thus the version should not be bumped.", thirdResponse.getVersion(), secondResponse.getVersion());
		assertThat(secondResponse.getVersion()).as("No new version number should be generated").isEqualTo(secondResponse.getVersion());
	}

	@Override
	public NodeResponse createNodeWithField() {
		HtmlFieldListImpl listField = new HtmlFieldListImpl();
		listField.add("A");
		listField.add("B");
		listField.add("C");

		return createNode(FIELD_NAME, listField);
	}

}
