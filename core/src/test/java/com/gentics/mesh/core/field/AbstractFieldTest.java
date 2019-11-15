package com.gentics.mesh.core.field;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerVersionImpl;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.FieldMapImpl;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.list.FieldList;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.util.Tuple;

import org.mockito.Mockito;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public abstract class AbstractFieldTest<FS extends FieldSchema> extends AbstractMeshTest implements FieldTestcases {

	abstract protected FS createFieldSchema(boolean isRequired);

	protected Tuple<Node, NodeGraphFieldContainer> createNode(boolean isRequiredField, String segmentField) {
		SchemaContainer container = Tx.get().getGraph().addFramedVertex(SchemaContainerImpl.class);
		SchemaContainerVersionImpl version = Tx.get().getGraph().addFramedVertex(SchemaContainerVersionImpl.class);
		version.setSchemaContainer(container);
		container.setLatestVersion(version);
		SchemaModel schema = new SchemaModelImpl();
		schema.setName("dummySchema");
		schema.addField(createFieldSchema(isRequiredField));
		if (segmentField != null) {
			schema.setSegmentField(segmentField);
		}
		version.setSchema(schema);
		Node node = project().getNodeRoot().create(user(), version, project());
		node.setParentNode(initialBranchUuid(), project().getBaseNode());
		EventQueueBatch batch = Mockito.mock(EventQueueBatch.class);
		initialBranch().assignSchemaVersion(user(), version, batch);
		NodeGraphFieldContainer nodeContainer = node.createGraphFieldContainer(english(), initialBranch(), user());

		return Tuple.tuple(node, nodeContainer);
	}

	protected NodeResponse transform(Node node) throws Exception {
		String json = getJson(node);
		assertNotNull(json);
		NodeResponse response = JsonUtil.readValue(json, NodeResponse.class);
		assertNotNull(response);
		return response;
	}

	protected Schema prepareNode(Node node, String listName, String listType) {
		SchemaModel schema = node.getSchemaContainer().getLatestVersion().getSchema();
		ListFieldSchema nodeListFieldSchema = new ListFieldSchemaImpl();
		nodeListFieldSchema.setName(listName);
		nodeListFieldSchema.setListType(listType);
		schema.addField(nodeListFieldSchema);
		node.getSchemaContainer().getLatestVersion().setSchema(schema);
		return schema;
	}

	protected void assertList(int expectedItems, String fieldKey, String listType, NodeResponse response) {
		Field deserializedList = response.getFields().getField(fieldKey, FieldTypes.LIST, listType, false);
		assertNotNull(deserializedList);
		FieldList<?> listField = (FieldList<?>) deserializedList;
		assertEquals("The list of type {" + listType + "} did not contain the expected amount of items.", expectedItems, listField.getItems().size());
	}

	protected void invokeRemoveFieldViaNullTestcase(String fieldName, FieldFetcher fetcher, DataProvider dummyFieldCreator,
			Consumer<NodeGraphFieldContainer> updater) {
		NodeGraphFieldContainer container = createNode(false, null).v2();
		dummyFieldCreator.set(container, fieldName);
		updater.accept(container);
		assertNull("The field should have been deleted by setting it to null", fetcher.fetch(container, fieldName));
	}

	protected void invokeUpdateFromRestTestcase(String fieldName, FieldFetcher fetcher, DataProvider createEmpty) {
		InternalActionContext ac = mockActionContext();
		NodeGraphFieldContainer container = createNode(false, null).v2();
		updateContainer(ac, container, fieldName, null);
		assertNull("No field should have been created", fetcher.fetch(container, fieldName));
	}

	/**
	 * 
	 * @param fieldName
	 * @param fetcher
	 * @param createDummyData
	 *            Data provider which creates some initial dummy data within the node
	 * @param updater
	 *            Action which updates the given node using a null value
	 */
	protected void invokeRemoveRequiredFieldViaNullTestcase(String fieldName, FieldFetcher fetcher, DataProvider createDummyData,
			Consumer<NodeGraphFieldContainer> updater) {
		NodeGraphFieldContainer container = createNode(true, null).v2();
		createDummyData.set(container, fieldName);
		try {
			updater.accept(container);
			fail("The update should have failed");
		} catch (GenericRestException e) {
			assertEquals("node_error_required_field_not_deletable", e.getI18nKey());
			assertThat(e.getI18nParameters()).containsExactly(fieldName, "dummySchema");
		}
	}

	protected void invokeRemoveSegmentFieldViaNullTestcase(String fieldName, FieldFetcher fetcher, DataProvider createDummyData,
			Consumer<NodeGraphFieldContainer> updater) {
		NodeGraphFieldContainer container = createNode(false, fieldName).v2();
		createDummyData.set(container, fieldName);
		updater.accept(container);
	}

	protected void invokeUpdateFromRestNullOnCreateRequiredTestcase(String fieldName, FieldFetcher fetcher) {
		invokeUpdateFromRestNullOnCreateRequiredTestcase(fieldName, fetcher, true);
	}

	/**
	 * Invoke the test case.
	 * 
	 * @param fieldName
	 *            Name/key of the field which will be updated
	 * @param fetcher
	 * @param expectError
	 */
	protected void invokeUpdateFromRestNullOnCreateRequiredTestcase(String fieldName, FieldFetcher fetcher, boolean expectError) {
		NodeGraphFieldContainer container = createNode(true, null).v2();
		try {
			InternalActionContext ac = mockActionContext();
			updateContainer(ac, container, fieldName, null);
			if (expectError) {
				fail("The update should have failed but it did not.");
			}
		} catch (GenericRestException e) {
			assertEquals("node_error_missing_required_field_value", e.getI18nKey());
			assertThat(e.getI18nParameters()).containsExactly(fieldName, "dummySchema");

			// verify that the container was not modified
			assertNull("No field should have been created", fetcher.fetch(container, fieldName));
		}
	}

	/**
	 * Invoke the update testcase using the provides data.
	 * 
	 * @param fieldName
	 *            Key of the testfield which will be created
	 * @param createDummyData
	 *            Data provider which will create a initial value
	 * @param updater
	 *            Action which will update the field
	 * @param asserter
	 *            Action which will assert the update
	 */
	protected void invokeUpdateFromRestValidSimpleValueTestcase(String fieldName, DataProvider createDummyData,
			Consumer<NodeGraphFieldContainer> updater, Consumer<NodeGraphFieldContainer> asserter) {
		NodeGraphFieldContainer container = createNode(false, null).v2();
		createDummyData.set(container, fieldName);
		updater.accept(container);
		asserter.accept(container);
	}

	/**
	 * Update a node container using a field map which contains the provided field.
	 * 
	 * @param ac
	 * @param node
	 *            Node to be used for update
	 * @param fieldKey
	 *            Field key to be used when adding field to update model
	 * @param field
	 *            Field to be added to the update model
	 * @return
	 */
	protected void updateContainer(InternalActionContext ac, NodeGraphFieldContainer container, String fieldKey, Field field) {
		FieldMap fieldMap = new FieldMapImpl();
		fieldMap.put(fieldKey, field);
		container.updateFieldsFromRest(ac, fieldMap);
	}

}
