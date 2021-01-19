package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;

/**
 * Updater of a field which uses the REST field information from the fieldmap.
 */
@FunctionalInterface
public interface FieldUpdater {

	/**
	 * Update the field within the given container with the given key using the fieldmap data. The fieldmap data may indicate that the field has been set to
	 * null. This would lead to a deletion of the field if the schema permits this. Deletion may fail if the field is a required field.
	 * 
	 * @param container
	 * @param ac
	 * @param fieldMap
	 *            Fieldmap which contains the field data
	 * @param fieldKey
	 *            Key of the field
	 * @param fieldSchema
	 *            Schema of the field
	 * @param schema
	 */
	void update(GraphFieldContainer container, InternalActionContext ac, FieldMap fieldMap, String fieldKey, FieldSchema fieldSchema,
		FieldSchemaContainer schema);
}
