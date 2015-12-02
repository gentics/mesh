package com.gentics.mesh.core.data.node;

import java.util.Map;

import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.MicroschemaContainer;
import com.gentics.mesh.core.data.TransformableNode;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.handler.InternalActionContext;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

public interface Micronode extends GraphFieldContainer, MeshVertex, TransformableNode<MicronodeResponse> {
	public static final String TYPE = "micronode";

	/**
	 * Locate the field with the given fieldkey in this container and return the rest model for this field.
	 * 
	 * @param ac
	 * @param fieldKey
	 * @param fieldSchema
	 * @param handler
	 */
	void getRestFieldFromGraph(InternalActionContext ac, String fieldKey, FieldSchema fieldSchema,
			Handler<AsyncResult<Field>> handler);

	/**
	 * Use the given map of rest fields and the schema information to set the data from the map to this container.
	 * 
	 * @param ac
	 * @param fields
	 * @throws MeshSchemaException
	 */
	void updateFieldsFromRest(ActionContext ac, Map<String, Field> fields) throws MeshSchemaException;

	/**
	 * Return the microschema container that holds the microschema that is used in combination with this micronode.
	 * 
	 * @return microschema container
	 */
	MicroschemaContainer getMicroschemaContainer();

	/**
	 * Set the microschema container that is used in combination with this micronode.
	 * 
	 * @param microschema microschema container
	 */
	void setMicroschemaContainer(MicroschemaContainer microschema);

	/**
	 * Shortcut method for getMicroschemaContainer().getMicroschema()
	 * 
	 * @return microschema
	 */
	Microschema getMicroschema();

}
