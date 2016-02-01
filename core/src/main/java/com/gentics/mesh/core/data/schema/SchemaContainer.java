package com.gentics.mesh.core.data.schema;

import java.util.List;

import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.ReferenceableElement;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.SchemaResponse;

/**
 * A schema container is a graph element which stores the JSON schema data.
 */
public interface SchemaContainer extends MeshCoreVertex<SchemaResponse, SchemaContainer>, ReferenceableElement<SchemaReference> {

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

	/**
	 * Return the schema version.
	 * 
	 * @return
	 */
	String getVersion();

	/**
	 * Set the schema version.
	 * 
	 * @param version
	 */
	void setVersion(String version);

	/**
	 * Return the next version of this schema.
	 * 
	 * @return
	 */
	SchemaContainer getNextVersion();

	/**
	 * Set the next version of the schema container.
	 * 
	 * @param container
	 * @return Fluent API
	 */
	SchemaContainer setNextVersion(SchemaContainer container);

	/**
	 * Return the previous version of this schema.
	 * 
	 * @return
	 */
	SchemaContainer getPreviousVersion();

	/**
	 * Set the previous version of the container.
	 * 
	 * @param container
	 * @return Fluent API
	 */
	SchemaContainer setPreviousVersion(SchemaContainer container);

	/**
	 * Return the change for the previous version of the schema. Normally the previous change was used to build the schema.
	 * 
	 * @return
	 */
	SchemaChange getPreviousChange();

	/**
	 * Return the change for the next version.
	 * 
	 * @return Can be null if no further changes exist
	 */
	SchemaChange getNextChange();

	/**
	 * Set the next change for the schema. The next change is the first change in the chain of changes that lead to the new schema version.
	 * 
	 * @param change
	 * @return
	 */
	SchemaContainer setNextChange(SchemaChange change);

	/**
	 * Set the previous change for the schema. The previous change is the last change in the chain of changes that was used to create the schema container.
	 * 
	 * @param change
	 * @return
	 */
	SchemaContainer setPreviousChange(SchemaChange change);

}
