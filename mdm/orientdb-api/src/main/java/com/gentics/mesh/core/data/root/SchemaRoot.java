package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;

/**
 * A schema container root is an aggregation vertex which is used to aggregate schema container vertices.
 */
public interface SchemaRoot extends RootVertex<Schema> {

	public static final String TYPE = "schemas";


	/**
	 * Add the schema to the aggregation node and assign it to all branches of the project to which the root belongs.
	 * 
	 * @param user
	 *            User for the creator value of the jobs which will be created
	 * @param schemaContainer
	 * @param batch
	 */
	void addSchemaContainer(HibUser user, Schema schemaContainer, EventQueueBatch batch);

	/**
	 * Remove the schema container from the aggregation node.
	 * 
	 * @param schemaContainer
	 * @param batch
	 */
	void removeSchemaContainer(HibSchema schemaContainer, EventQueueBatch batch);

	/**
	 * Check whether the given schema is assigned to this root node.
	 * 
	 * @param schema
	 * @return
	 */
	boolean contains(HibSchema schema);

	/**
	 * Returns the project to which the schema container root belongs.
	 * 
	 * @return Project or null if this is the global root container
	 */
	Project getProject();

	Schema create();

	SchemaVersion createVersion();

	/**
	 * Return a list of all schema container roots to which the schema container was added.
	 *
	 * @return
	 */
	Result<? extends SchemaRoot> getRoots(Schema schema);

	/**
	 * Returns an iterable of nodes which are referencing the schema container.
	 *
	 * @return
	 */
	Result<? extends Node> getNodes(Schema schema);

	/**
	 * Return an iterable with all found schema versions.
	 *
	 * @return
	 */
	Iterable<? extends SchemaVersion> findAllVersions(Schema schema);
}
