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
	 * Return the previous version of this schema.
	 * 
	 * @return
	 */
	SchemaContainer getPreviousVersion();

	/**
	 * Return the changeset for the previous version. Normally the previous changeset is used to build the current version of the schema.
	 * 
	 * @return
	 */
	SchemaChangeset getChangesetForPreviousVersion();

	/**
	 * Return the changeset for the next version.
	 * 
	 * @return
	 */
	SchemaChangeset getChangesetForNextVersion();

}
