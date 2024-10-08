package com.gentics.mesh.core.field.number;

import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;

import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.field.AbstractListFieldEndpointTest;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.list.impl.NumberFieldListImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;

import io.reactivex.Completable;

@MeshTestSetting(testSize = TestSize.PROJECT_AND_NODE, startServer = true)
public class NumberFieldListEndpointTest extends AbstractListFieldEndpointTest {

	@Override
	public String getListFieldType() {
		return "number";
	}

	@Test
	@Override
	public void testCreateNodeWithField() {
		NodeResponse response = createNodeWithField();
		NumberFieldListImpl field = response.getFields().getNumberFieldList(FIELD_NAME);
		assertThat(field.getItems().stream().map(Number::doubleValue).collect(Collectors.toList())).as("Only valid values should be stored")
				.containsExactly(new BigDecimal(42).doubleValue(), new BigDecimal(41).doubleValue(), new BigDecimal(0.1).doubleValue(), new BigDecimal(Long.MAX_VALUE).doubleValue());
	}

	@Test
	@Override
	public void testNullValueInListOnCreate() {
		NumberFieldListImpl listField = new NumberFieldListImpl();
		listField.add(42);
		listField.add(41);
		listField.add(null);
		createNodeAndExpectFailure(FIELD_NAME, listField, BAD_REQUEST, "field_list_error_null_not_allowed", FIELD_NAME);
	}

	@Test
	@Override
	public void testNullValueInListOnUpdate() {
		NumberFieldListImpl listField = new NumberFieldListImpl();
		listField.add(42);
		listField.add(41);
		listField.add(null);
		updateNodeFailure(FIELD_NAME, listField, BAD_REQUEST, "field_list_error_null_not_allowed", FIELD_NAME);
	}

	@Test
	@Override
	public void testCreateNodeWithNoField() {
		NodeResponse response = createNode(FIELD_NAME, (Field) null);
		assertThat(response.getFields().getNumberFieldList(FIELD_NAME)).as("List field in reponse should be null").isNull();
	}

	@Test
	@Override
	public void testUpdateSameValue() {
		NumberFieldListImpl listField = new NumberFieldListImpl();
		listField.add(41L);
		listField.add(42L);

		NodeResponse firstResponse = updateNode(FIELD_NAME, listField);
		String oldVersion = firstResponse.getVersion();

		NodeResponse secondResponse = updateNode(FIELD_NAME, listField);
		assertThat(secondResponse.getVersion()).as("New version number").isEqualTo(oldVersion);
	}

	@Test
	@Override
	public void testReadNodeWithExistingField() {
		// 1. Update an existing node
		NumberFieldListImpl listField = new NumberFieldListImpl();
		listField.add(41L);
		listField.add(42L);
		NodeResponse firstResponse = updateNode(FIELD_NAME, listField);

		// 2. Read the node
		NodeResponse response = readNode(PROJECT_NAME, firstResponse.getUuid());
		NumberFieldListImpl deserializedField = response.getFields().getNumberFieldList(FIELD_NAME);
		assertNotNull(deserializedField);
		assertThat(deserializedField.getItems().stream().map(Number::intValue).collect(Collectors.toList())).as("List field values from updated node (null values are omitted)").containsExactly(41, 42);
	}

	@Test
	@Override
	public void testUpdateNodeFieldWithField() throws IOException {
		disableAutoPurge();

		HibNode node = folder("2015");

		List<List<Number>> valueCombinations = Arrays.asList(Arrays.asList(1.1, 2, 3), Arrays.asList(3, 2, 1.1), Collections.emptyList(),
			Arrays.asList(47.11, 8.15), Arrays.asList(3));

		HibNodeFieldContainer container = tx(tx -> { return tx.contentDao().getFieldContainer(node, "en"); });
		for (int i = 0; i < 20; i++) {
			final HibNodeFieldContainer currentContainer = container;
			List<Number> oldValue = tx(() -> getListValues(currentContainer::getNumberList, FIELD_NAME));
			List<Number> newValue = valueCombinations.get(i % valueCombinations.size());

			NumberFieldListImpl list = new NumberFieldListImpl();
			for (Number value : newValue) {
				list.add(value);
			}
			NodeResponse response = updateNode(FIELD_NAME, list);
			NumberFieldListImpl field = response.getFields().getNumberFieldList(FIELD_NAME);
			assertThat(field.getItems().stream().map(Number::doubleValue).collect(Collectors.toList())).as("Updated field")
					.containsExactlyElementsOf(list.getItems().stream().map(Number::doubleValue).collect(Collectors.toList()));

			try (Tx tx = tx()) {
				ContentDao contentDao = tx.contentDao();
				HibNodeFieldContainer newContainerVersion = contentDao.getNextVersions(container).iterator().next();
				assertEquals("Check version number", newContainerVersion.getVersion().toString(), response.getVersion());
				assertEquals("Check old value", oldValue, getListValues(container::getNumberList, FIELD_NAME));
				container = newContainerVersion;
			}
		}
	}

	@Test
	@Override
	public void testUpdateSetNull() {
		disableAutoPurge();

		HibNode node = folder("2015");

		NumberFieldListImpl list = new NumberFieldListImpl();
		list.add(42);
		list.add(41.1f);
		NodeResponse firstResponse = updateNode(FIELD_NAME, list);
		String oldVersion = firstResponse.getVersion();

		NodeResponse secondResponse = updateNode(FIELD_NAME, null);
		assertThat(secondResponse.getFields().getNumberFieldList(FIELD_NAME)).as("Updated Field").isNull();
		assertThat(oldVersion).as("Version should be updated").isNotEqualTo(secondResponse.getVersion());

		// Assert that the old version was not modified
		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();
			HibNodeFieldContainer latest = contentDao.getLatestDraftFieldContainer(node, english());
			assertThat(latest.getVersion().toString()).isEqualTo(secondResponse.getVersion());
			assertThat(latest.getNumberList(FIELD_NAME)).isNull();
			assertThat(latest.getPreviousVersion().getNumberList(FIELD_NAME)).isNotNull();
			List<Number> oldValueList = latest.getPreviousVersion().getNumberList(FIELD_NAME).getList().stream().map(item -> item.getNumber().doubleValue())
				.collect(Collectors.toList());
			assertThat(oldValueList).containsExactly(42.0, 41.1);
		}

		NodeResponse thirdResponse = updateNode(FIELD_NAME, null);
		assertEquals("The field does not change and thus the version should not be bumped.", thirdResponse.getVersion(),
			secondResponse.getVersion());
	}

	@Test
	@Override
	public void testUpdateSetEmpty() {
		NumberFieldListImpl list = new NumberFieldListImpl();
		list.add(42);
		list.add(41.1f);
		NodeResponse firstResponse = updateNode(FIELD_NAME, list);
		String oldVersion = firstResponse.getVersion();

		NumberFieldListImpl emptyField = new NumberFieldListImpl();
		NodeResponse secondResponse = updateNode(FIELD_NAME, emptyField);
		assertThat(secondResponse.getFields().getNumberFieldList(FIELD_NAME)).as("Updated field list").isNotNull();
		assertThat(secondResponse.getFields().getNumberFieldList(FIELD_NAME).getItems()).as("Field value should be truncated").isEmpty();
		assertThat(secondResponse.getVersion()).as("New version number should be generated").isNotEqualTo(oldVersion);

		NodeResponse thirdResponse = updateNode(FIELD_NAME, emptyField);
		assertEquals("The field does not change and thus the version should not be bumped.", thirdResponse.getVersion(), secondResponse.getVersion());
		assertThat(secondResponse.getVersion()).as("No new version number should be generated").isEqualTo(secondResponse.getVersion());
	}

	@Test
	public void testListOrder() throws Exception {
		final int elementCount = 50;
		NumberFieldListImpl listField = new NumberFieldListImpl();
		NodeResponse response = createNode(FIELD_NAME, listField);

		addNumbers(response, elementCount).blockingAwait();
		response = client().findNodeByUuid(PROJECT_NAME, response.getUuid()).toSingle().blockingGet();

		Integer[] expected = IntStream.range(0, elementCount)
			.boxed()
			.toArray(Integer[]::new);

		List<Integer> actual = response.getFields().getNumberFieldList(FIELD_NAME)
			.getItems().stream().map(Number::intValue).collect(Collectors.toList());

		assertThat(actual).containsExactly(expected);
	}

	/**
	 * Adds numbers to a number field list of node until the count is reached.
	 * 
	 * @param node
	 * @param count
	 * @return
	 */
	private Completable addNumbers(NodeResponse node, int count) {
		FieldMap fields = node.getFields();
		NumberFieldListImpl numbers = fields.getNumberFieldList(FIELD_NAME);
		if (numbers.getItems().size() < count) {
			int nr;
			if (numbers.getItems().size() == 0) {
				nr = 0;
			} else {
				nr = numbers.getItems().get(numbers.getItems().size() - 1).intValue() + 1;
			}
			numbers.add(nr);
			fields.put(FIELD_NAME, numbers);
			NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest()
				.setFields(fields)
				.setLanguage(node.getLanguage())
				.setVersion(node.getVersion());
			return client().updateNode(PROJECT_NAME, node.getUuid(), nodeUpdateRequest).toSingle()
				.flatMapCompletable(nodeRes -> addNumbers(nodeRes, count));
		} else {
			return Completable.complete();
		}
	}

	@Override
	public NodeResponse createNodeWithField() {
		NumberFieldListImpl listField = new NumberFieldListImpl();
		listField.add(42L);
		listField.add(41L);
		listField.add(0.1f);
		listField.add(Long.MAX_VALUE);

		return createNode(FIELD_NAME, listField);
	}
}
