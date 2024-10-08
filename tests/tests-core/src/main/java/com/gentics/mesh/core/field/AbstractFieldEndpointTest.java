package com.gentics.mesh.core.field;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.Test;

import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.list.HibListField;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.TestHelper;

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
		if (field != null) {
			try (Tx tx = tx()) {
				HibNode node = folder("2015");
				prepareTypedSchema(node, TestHelper.fieldIntoSchema(field).setName(fieldKey), true);
				tx.success();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setParentNodeUuid(folder("2015").getUuid());
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
		tx(tx -> {
			prepareTypedSchema(schemaContainer("folder"), Optional.ofNullable(field).stream()
				.map(TestHelper::fieldIntoSchema)
				.map(schema -> schema.setName(fieldKey)).collect(Collectors.toList()), Optional.empty()); 
			tx.success();
		});
		NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
		nodeUpdateRequest.setLanguage("en");
		nodeUpdateRequest.getFields().put(fieldKey, field);
		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();
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
		tx(tx -> { 
			prepareTypedSchema(node, TestHelper.fieldIntoSchema(field).setName(fieldKey), true); 
			tx.success();
		});
		NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
		nodeUpdateRequest.setLanguage("en");
		nodeUpdateRequest.getFields().put(fieldKey, field);
		nodeUpdateRequest.setVersion(tx(tx -> {
			return tx.contentDao().getLatestDraftFieldContainer(node, english()).getVersion().toString();
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
	protected <U, T extends HibListField<?, ?, U>> List<U> getListValues(Function<String, T> getter, String fieldKey) {
		T field = getter.apply(fieldKey);
		return field != null ? field.getValues() : null;
	}

	@Test
	@Override
	public void testDeleteField() {
		NodeResponse response = createNodeWithField();
		call(() -> client().deleteNode(PROJECT_NAME, response.getUuid()));
	}
}
