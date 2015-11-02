package com.gentics.mesh.core.data;

import java.util.List;

import com.gentics.mesh.core.data.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.SchemaResponse;
import com.gentics.mesh.handler.InternalActionContext;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

/**
 * A schema container is a graph element which stores the JSON schema data.
 *
 */
public interface SchemaContainer extends GenericVertex<SchemaResponse>, NamedVertex, IndexedVertex, ReferenceableElement<SchemaReference> {

	public static final String TYPE = "schemaContainer";

	/**
	 * Return the schema that is stored within the container
	 * 
	 * @return
	 */
	Schema getSchema();

	/**
	 * Set the schema for the container
	 * 
	 * @param schema
	 */
	void setSchema(Schema schema);

	/**
	 * Return a list of nodes to which the schema has been assigned.
	 * 
	 * @return
	 */
	List<? extends Node> getNodes();

//	/**
//	 * Transform the schema container to a schema reference.
//	 * 
//	 * @param ac
//	 * @param handler
//	 * @return
//	 */
//	//SchemaContainer transformToReference(InternalActionContext ac, Handler<AsyncResult<SchemaReference>> handler);

//	SchemaContainerImpl getImpl();

}
