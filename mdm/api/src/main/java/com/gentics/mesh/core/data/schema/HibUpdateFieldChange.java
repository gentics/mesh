package com.gentics.mesh.core.data.schema;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.ELASTICSEARCH_KEY;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.Collections;
import java.util.Map;

import com.gentics.mesh.core.rest.common.FieldContainer;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;

/**
 * Change entry which contains information for a field update. This includes field specific settings.
 */
public interface HibUpdateFieldChange extends HibSchemaFieldChange {

	SchemaChangeOperation OPERATION = SchemaChangeOperation.UPDATEFIELD;

	@Override
	default SchemaChangeOperation getOperation() {
		return OPERATION;
	}

	/**
	 * Return the field label.
	 * 
	 * @return
	 */
	default String getLabel() {
		return getRestProperty(SchemaChangeModel.LABEL_KEY);
	}

	/**
	 * Set the field label.
	 * 
	 * @param label
	 */
	default void setLabel(String label) {
		setRestProperty(SchemaChangeModel.LABEL_KEY, label);
	}

	@Override
	default <R extends FieldSchemaContainer> R apply(R container) {
		FieldSchema fieldSchema = container.getField(getFieldName());

		if (fieldSchema == null) {
			throw error(BAD_REQUEST, "schema_error_change_field_not_found", getFieldName(), container.getName(), getUuid());
		}
		fieldSchema.apply(getRestProperties());
		return container;
	}

	@Override
	default void updateFromRest(SchemaChangeModel restChange) {
		/***
		 * Many graph databases can't handle null values. Tinkerpop blueprint contains constrains which avoid setting null values. We store empty string for the
		 * segment field name instead. It is possible to set setStandardElementConstraints for each tx to false in order to avoid such checks.
		 */
		if (restChange.getProperties().containsKey(ELASTICSEARCH_KEY) && restChange.getProperty(ELASTICSEARCH_KEY) == null) {
			restChange.setProperty(ELASTICSEARCH_KEY, "{}");
		}
		HibSchemaFieldChange.super.updateFromRest(restChange);
	}

	@Override
	default Map<String, Field> createFields(FieldSchemaContainer oldSchema, FieldContainer oldContent) {
		String oldFieldName = getFieldName();
		String newFieldName = getRestProperty(SchemaChangeModel.NAME_KEY);
		if (oldFieldName != null && newFieldName != null) {
			// field name changed
			FieldSchema fieldSchema = oldSchema.getField(oldFieldName);
			Field field = oldContent.getFields().getField(oldFieldName, fieldSchema);
			return Collections.singletonMap(newFieldName, field);
		}

		if (oldFieldName != null) {
			// no field renaming, we can clone the old field as is
			FieldSchema fieldSchema = oldSchema.getField(oldFieldName);
			Field field = oldContent.getFields().getField(oldFieldName, fieldSchema);
			return Collections.singletonMap(oldFieldName, field);
		}

		return Collections.emptyMap();
	}
}
