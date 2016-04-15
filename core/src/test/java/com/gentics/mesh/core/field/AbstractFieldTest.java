package com.gentics.mesh.core.field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.AbstractBasicDBTest;
import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerVersionImpl;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.FieldMapJsonImpl;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.list.FieldList;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaModel;
import com.gentics.mesh.json.JsonUtil;

import rx.functions.Action1;
import rx.functions.Func1;

public abstract class AbstractFieldTest<FS extends FieldSchema> extends AbstractBasicDBTest implements FieldTestcases {

	abstract protected FS createFieldSchema(boolean isRequired);

	@Autowired
	protected ServerSchemaStorage schemaStorage;

	protected Node createNode(boolean isRequiredField) {
		SchemaContainer container = tx.getGraph().addFramedVertex(SchemaContainerImpl.class);
		SchemaContainerVersionImpl version = tx.getGraph().addFramedVertex(SchemaContainerVersionImpl.class);
		version.setSchemaContainer(container);
		container.setLatestVersion(version);
		Schema schema = new SchemaModel();
		schema.setName("dummySchema");
		schema.addField(createFieldSchema(isRequiredField));
		version.setSchema(schema);
		Node node = meshRoot().getNodeRoot().create(user(), version, project());
		//TODO fake valid release
		Release release =null;
		node.createGraphFieldContainer(english(), release, user());
		return node;
	}

	protected NodeResponse transform(Node node) throws Exception {
		String json = getJson(node);
		assertNotNull(json);
		NodeResponse response = JsonUtil.readValue(json, NodeResponse.class);
		assertNotNull(response);
		return response;
	}

	protected Schema prepareNode(Node node, String listName, String listType) {
		Schema schema = node.getSchemaContainer().getLatestVersion().getSchema();
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

	protected void invokeRemoveFieldViaNullValueTestcase(String fieldName, FieldFetcher fetcher,
			Func1<GraphFieldContainer, GraphField> dummyFieldCreator, Action1<Node> updater) {
		Node node = createNode(false);
		NodeGraphFieldContainer container = node.getGraphFieldContainer(english());
		dummyFieldCreator.call(container);
		updater.call(node);
		container.reload();
		assertNull("The field should have been deleted by setting it to null", fetcher.fetch(container, fieldName));
	}

	/**
	 * Update a node using a field map which contains the provided field.
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
	protected NodeGraphFieldContainer updateNode(InternalActionContext ac, Node node, String fieldKey, Field field) {
		NodeGraphFieldContainer container = node.getGraphFieldContainer(english());
		FieldMap fieldMap = new FieldMapJsonImpl();
		fieldMap.put(fieldKey, field);
		container.updateFieldsFromRest(ac, fieldMap, container.getSchemaContainerVersion().getSchema());
		container.reload();
		return container;
	}

}
