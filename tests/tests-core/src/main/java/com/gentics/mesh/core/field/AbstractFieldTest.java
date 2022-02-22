package com.gentics.mesh.core.field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.mockito.Mockito;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.BranchDao;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.dao.PersistingContentDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.FieldMapImpl;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.list.FieldList;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.util.Tuple;

public abstract class AbstractFieldTest<FS extends FieldSchema> extends AbstractMeshTest implements FieldTestcases {

	abstract protected FS createFieldSchema(boolean isRequired);
	
	protected PersistingContentDao contentDao() {
		return contentDao(Tx.get());
	}
	protected PersistingContentDao contentDao(Tx tx) {
		return tx.<CommonTx>unwrap().contentDao();
	}

	protected Tuple<HibNode, HibNodeFieldContainer> createNode(boolean isRequiredField, String segmentField) {
		NodeDao nodeDao = Tx.get().nodeDao();
		BranchDao branchDao = Tx.get().branchDao();

		SchemaVersionModel schema = new SchemaModelImpl();
		schema.setName("dummySchema");
		schema.addField(createFieldSchema(isRequiredField));
		if (segmentField != null) {
			schema.setSegmentField(segmentField);
		}
		HibSchema container = CommonTx.get().schemaDao().create(schema, null, null, false);
		HibSchemaVersion version = container.getLatestVersion();
		Tx.get().commit();
		HibNode node = nodeDao.create(project(), user(), version);
		HibBranch branch = branchDao.findByUuid(initialBranch().getProject(), initialBranch().getUuid());
		nodeDao.setParentNode(node, branch.getUuid(), nodeDao.findByUuidGlobal(project().getBaseNode().getUuid()));
		EventQueueBatch batch = Mockito.mock(EventQueueBatch.class);
		Tx.get().branchDao().assignSchemaVersion(branch, user(), version, batch);
		HibNodeFieldContainer nodeContainer = boot().contentDao().createFieldContainer(node, english(), branch, user());

		return Tuple.tuple(node, nodeContainer);
	}

	protected NodeResponse transform(HibNode node) throws Exception {
		String json = getJson(node);
		assertNotNull(json);
		NodeResponse response = JsonUtil.readValue(json, NodeResponse.class);
		assertNotNull(response);
		return response;
	}

	protected SchemaModel prepareNode(HibNode node, String listName, String listType) {
		ListFieldSchema nodeListFieldSchema = new ListFieldSchemaImpl();
		nodeListFieldSchema.setName(listName);
		nodeListFieldSchema.setListType(listType);
		try {
			prepareTypedSchema(node.getSchemaContainer(), List.of(nodeListFieldSchema), Optional.empty());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return node.getSchemaContainer().getLatestVersion().getSchema();
	}

	protected void assertList(int expectedItems, String fieldKey, String listType, NodeResponse response) {
		Field deserializedList = response.getFields().getField(fieldKey, FieldTypes.LIST, listType, false);
		assertNotNull(deserializedList);
		FieldList<?> listField = (FieldList<?>) deserializedList;
		assertEquals("The list of type {" + listType + "} did not contain the expected amount of items.", expectedItems, listField.getItems().size());
	}

	protected void invokeRemoveFieldViaNullTestcase(String fieldName, FieldFetcher fetcher, DataProvider dummyFieldCreator,
			Consumer<HibNodeFieldContainer> updater) {
		HibNodeFieldContainer container = createNode(false, null).v2();
		dummyFieldCreator.set(container, fieldName);
		updater.accept(container);
		assertNull("The field should have been deleted by setting it to null", fetcher.fetch(container, fieldName));
	}

	protected void invokeUpdateFromRestTestcase(String fieldName, FieldFetcher fetcher, DataProvider createEmpty) {
		InternalActionContext ac = mockActionContext();
		HibNodeFieldContainer container = createNode(false, null).v2();
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
			Consumer<HibNodeFieldContainer> updater) {
		HibNodeFieldContainer container = createNode(true, null).v2();
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
			Consumer<HibNodeFieldContainer> updater) {
		HibNodeFieldContainer container = createNode(false, fieldName).v2();
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
		HibNodeFieldContainer container = createNode(true, null).v2();
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
			Consumer<HibNodeFieldContainer> updater, Consumer<HibNodeFieldContainer> asserter) {
		HibNodeFieldContainer container = createNode(false, null).v2();
		createDummyData.set(container, fieldName);
		updater.accept(container);
		asserter.accept(container);
	}

	/**
	 * Update a node container using a field map which contains the provided field.
	 * 
	 * @param ac
	 * @param container
	 *            Node to be used for update
	 * @param fieldKey
	 *            Field key to be used when adding field to update model
	 * @param field
	 *            Field to be added to the update model
	 * @return
	 */
	protected void updateContainer(InternalActionContext ac, HibNodeFieldContainer container, String fieldKey, Field field) {
		FieldMap fieldMap = new FieldMapImpl();
		fieldMap.put(fieldKey, field);
		container.updateFieldsFromRest(ac, fieldMap);
	}

}
