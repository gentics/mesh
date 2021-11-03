package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.result.Result;

/**
 * A schema container root is an aggregation vertex which is used to aggregate schema container vertices.
 */
public interface SchemaRoot extends RootVertex<Schema> {

	public static final String TYPE = "schemas";

	/**
	 * Returns the project to which the schema container root belongs.
	 * 
	 * @return Project or null if this is the global root container
	 */
	Project getProject();

	/**
	 * Create a new schema.
	 * 
	 * @return
	 */
	Schema create();

	/**
	 * Create a new schema version.
	 * 
	 * @return
	 */
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

	/**
	 * Get schema version entity class.
	 * 
	 * @return
	 */
	Class<? extends SchemaVersion> getSchemaVersionPersistenceClass();
}
