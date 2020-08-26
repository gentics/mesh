package com.gentics.mesh.core.field;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.dao.ContentDaoWrapper;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.list.ListGraphField;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;

import io.netty.handler.codec.http.HttpResponseStatus;

public abstract class AbstractFieldEndpointTest extends AbstractMeshTest implements FieldEndpointTestcases {

	protected NodeResponse readNode(HibNode node, String... expandedFieldNames) {
		NodeParametersImpl parameters = new NodeParametersImpl();
		parameters.setLanguages("en");
		parameters.setExpandedFieldNames(expandedFieldNames);
		return call(() -> client().findNodeByUuid(PROJECT_NAME, tx(() -> node.getUuid()), parameters, new VersioningParametersImpl().draft()));
	}

	protected void createNodeAndExpectFailure(String fieldKey, Field field, HttpResponseStatus status, String bodyMessageI18nKey,
		String... i18nParams) {
		HibNode node = folder("2015");
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setParentNodeUuid(node.getUuid());
		nodeCreateRequest.setSchema(new SchemaReferenceImpl().setName("folder"));
		nodeCreateRequest.setLanguage("en");
		if (fieldKey != null) {
			nodeCreateRequest.getFields().put(fieldKey, field);
		}

		call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest, new NodeParametersImpl().setLanguages("en")), status, bodyMessageI18nKey,
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
		NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
		nodeUpdateRequest.setLanguage("en");
		nodeUpdateRequest.getFields().put(fieldKey, field);
		try (Tx tx = tx()) {
			ContentDaoWrapper contentDao = tx.data().contentDao();
			HibNode node = folder("2015");
			nodeUpdateRequest.setVersion(contentDao.getLatestDraftFieldContainer(node, english()).getVersion().toString());
			tx.success();
		}
		String uuid = tx(() -> folder("2015").getUuid());
		NodeResponse response = call(() -> client().updateNode(PROJECT_NAME, uuid, nodeUpdateRequest, new NodeParametersImpl().setLanguages("en")));
		assertNotNull("The response could not be found in the result of the future.", response);
		assertNotNull("The field was not included in the response.", response.getFields().hasField(fieldKey));
		return response;
	}

	protected void updateNodeFailure(String fieldKey, Field field, HttpResponseStatus status, String bodyMessageI18nKey, String... i18nParams) {
		HibNode node = folder("2015");
		NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
		nodeUpdateRequest.setLanguage("en");
		nodeUpdateRequest.getFields().put(fieldKey, field);
		nodeUpdateRequest.setVersion(tx(tx -> {
			return tx.data().contentDao().getLatestDraftFieldContainer(node, english()).getVersion().toString();
		}));

		call(() -> client().updateNode(PROJECT_NAME, tx(() -> node.getUuid()), nodeUpdateRequest, new NodeParametersImpl().setLanguages("en")),
			status, bodyMessageI18nKey, i18nParams);
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
		return field != null ? field.getValues() : null;
	}

	@Test
	@Override
	public void testDeleteField() {
		NodeResponse response = createNodeWithField();
		call(() -> client().deleteNode(PROJECT_NAME, response.getUuid()));
	}
}
