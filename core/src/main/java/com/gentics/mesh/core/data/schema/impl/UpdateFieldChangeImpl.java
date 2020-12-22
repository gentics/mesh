package com.gentics.mesh.core.data.schema.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.ELASTICSEARCH_KEY;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.schema.UpdateFieldChange;
import com.gentics.mesh.core.rest.common.FieldContainer;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;

/**
 * @see UpdateFieldChange
 */
public class UpdateFieldChangeImpl extends AbstractSchemaFieldChange implements UpdateFieldChange {

	/**
	 * Initialize the vertex type and index.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(UpdateFieldChangeImpl.class, MeshVertexImpl.class);
	}

	@Override
	public SchemaChangeOperation getOperation() {
		return OPERATION;
	}

	@Override
	public String getLabel() {
		return getRestProperty(SchemaChangeModel.LABEL_KEY);
	}

	@Override
	public void setLabel(String label) {
		setRestProperty(SchemaChangeModel.LABEL_KEY, label);
	}

	@Override
	public FieldSchemaContainer apply(FieldSchemaContainer container) {
		FieldSchema fieldSchema = container.getField(getFieldName());

		if (fieldSchema == null) {
			throw error(BAD_REQUEST, "schema_error_change_field_not_found", getFieldName(), container.getName(), getUuid());
		}
		// Remove prefix from map keys
		Map<String, Object> properties = new HashMap<>();
		for (String key : getRestProperties().keySet()) {
			Object value = getRestProperties().get(key);
			key = key.replace(REST_PROPERTY_PREFIX_KEY, "");
			properties.put(key, value);
		}

		fieldSchema.apply(properties);
		return container;
	}

	@Override
	public void updateFromRest(SchemaChangeModel restChange) {
		/***
		 * Many graph databases can't handle null values. Tinkerpop blueprint contains constrains which avoid setting null values. We store empty string for the
		 * segment field name instead. It is possible to set setStandardElementConstraints for each tx to false in order to avoid such checks.
		 */
		if (restChange.getProperties().containsKey(ELASTICSEARCH_KEY) && restChange.getProperty(ELASTICSEARCH_KEY) == null) {
			restChange.setProperty(ELASTICSEARCH_KEY, "{}");
		}
		super.updateFromRest(restChange);
	}

	@Override
	public Map<String, Field> createFields(FieldSchemaContainer oldSchema, FieldContainer oldContent) {
		String oldFieldName = getFieldName();
		String newFieldName = getRestProperty(SchemaChangeModel.NAME_KEY);
		if (oldFieldName != null && newFieldName != null) {
			FieldSchema fieldSchema = oldSchema.getField(oldFieldName);
			Field field = oldContent.getFields().getField(oldFieldName, fieldSchema);
			return Collections.singletonMap(newFieldName, field);
		} else {
			return Collections.emptyMap();
		}
	}

	@Override
	public void delete(BulkActionContext bac) {
		getElement().remove();
	}

}
