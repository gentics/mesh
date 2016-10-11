package com.gentics.mesh.core.field;

import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.list.ListGraphField;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.VersionReference;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.parameter.impl.NodeParameters;
import com.gentics.mesh.parameter.impl.VersioningParameters;
import com.gentics.mesh.test.AbstractRestEndpointTest;

import io.netty.handler.codec.http.HttpResponseStatus;

public abstract class AbstractFieldEndpointTest extends AbstractRestEndpointTest implements FieldEndpointTestcases {

	protected NodeResponse readNode(Node node, String... expandedFieldNames) {
		NodeParameters parameters = new NodeParameters();
		parameters.setLanguages("en");
		parameters.setExpandedFieldNames(expandedFieldNames);
		return call(() -> getClient().findNodeByUuid(PROJECT_NAME, node.getUuid(), parameters, new VersioningParameters().draft()));
	}

	protected void createNodeAndExpectFailure(String fieldKey, Field field, HttpResponseStatus status, String bodyMessageI18nKey,
			String... i18nParams) {
		Node node = folder("2015");
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setParentNodeUuid(node.getUuid());
		nodeCreateRequest.setSchema(new SchemaReference().setName("folder"));
		nodeCreateRequest.setLanguage("en");
		if (fieldKey != null) {
			nodeCreateRequest.getFields().put(fieldKey, field);
		}

		call(() -> getClient().createNode(PROJECT_NAME, nodeCreateRequest, new NodeParameters().setLanguages("en")), status, bodyMessageI18nKey,
				i18nParams);
	}

	/**
	 * Update the test node using the provided field field and field key as update data.
	 * 
	 * @param fieldKey
	 * @param field
	 * @return
	 */
	protected NodeResponse updateNode(String fieldKey, Field field) {
		return updateNode(fieldKey, field, false);
	}

	/**
	 * Update the test node using the provided field field and field key as update data.
	 * 
	 * @param fieldKey
	 * @param field
	 * @param expandAll
	 * @return
	 */
	protected NodeResponse updateNode(String fieldKey, Field field, boolean expandAll) {
		Node node = folder("2015");
		NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
		nodeUpdateRequest.setSchema(new SchemaReference().setName("folder"));
		nodeUpdateRequest.setLanguage("en");
		nodeUpdateRequest.getFields().put(fieldKey, field);
		node.reload();
		nodeUpdateRequest.setVersion(new VersionReference().setNumber(node.getLatestDraftFieldContainer(english()).getVersion().toString()));

		NodeResponse response = call(
				() -> getClient().updateNode(PROJECT_NAME, node.getUuid(), nodeUpdateRequest, new NodeParameters().setLanguages("en")));
		assertNotNull("The response could not be found in the result of the future.", response);
		assertNotNull("The field was not included in the response.", response.getFields().hasField(fieldKey));
		return response;
	}

	protected void updateNodeFailure(String fieldKey, Field field, HttpResponseStatus status, String bodyMessageI18nKey, String... i18nParams) {
		Node node = folder("2015");
		NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
		nodeUpdateRequest.setSchema(new SchemaReference().setName("folder"));
		nodeUpdateRequest.setLanguage("en");
		nodeUpdateRequest.getFields().put(fieldKey, field);
		nodeUpdateRequest.setVersion(new VersionReference().setNumber(node.getLatestDraftFieldContainer(english()).getVersion().toString()));

		call(() -> getClient().updateNode(PROJECT_NAME, node.getUuid(), nodeUpdateRequest, new NodeParameters().setLanguages("en")), status,
				bodyMessageI18nKey, i18nParams);
	}

	/**
	 * Get the values of the field
	 * 
	 * @param container
	 *            container
	 * @param classOfT
	 *            field class
	 * @param fieldKey
	 *            field name
	 * @return values or null
	 */
	protected <U, T extends ListGraphField<?, ?, U>> List<U> getListValues(NodeGraphFieldContainer container, Class<T> classOfT, String fieldKey) {
		T field = container.getList(classOfT, fieldKey);
		if (field != null) {
			field.reload();
		}
		return field != null ? field.getValues() : null;
	}
}
