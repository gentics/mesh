package com.gentics.mesh.core.data;

import java.util.Map;

import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.handler.InternalActionContext;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

/**
 * A node field container is a aggregation node that holds localized fields.
 *
 */
public interface NodeGraphFieldContainer extends GraphFieldContainer, MicroschemaGraphFieldContainer {

	/**
	 * Locate the field with the given fieldkey in this container and return the rest model for this field.
	 * 
	 * @param ac
	 * @param fieldKey
	 * @param fieldSchema
	 * @param expandField
	 * @param handler
	 */
	void getRestFieldFromGraph(InternalActionContext ac, String fieldKey, FieldSchema fieldSchema, boolean expandField, Handler<AsyncResult<Field>> handler);

	/**
	 * Use the given map of rest fields and the schema information to set the data from the map to this container.
	 * 
	 * @param ac
	 * @param fields
	 * @param schema
	 * @throws MeshSchemaException
	 */
	void updateFieldsFromRest(ActionContext ac, Map<String, Field> fields, Schema schema) throws MeshSchemaException;

	/**
	 * Delete the field container. This will also delete linked elements like lists
	 */
	void delete();

}
