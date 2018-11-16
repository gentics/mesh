package com.gentics.mesh.core.data.schema.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.schema.UpdateFieldChange;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;
import com.gentics.mesh.graphdb.spi.Database;

/**
 * @see UpdateFieldChange
 */
public class UpdateFieldChangeImpl extends AbstractSchemaFieldChange implements UpdateFieldChange {

	public static void init(Database database) {
		database.addVertexType(UpdateFieldChangeImpl.class, MeshVertexImpl.class);
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

}
