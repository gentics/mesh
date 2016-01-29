package com.gentics.mesh.core.data.schema;

import java.util.List;

import com.gentics.mesh.core.data.MeshVertex;

/**
 * A {@link SchemaChangeset} is the aggregation element for multiple {@link SchemaChange} objects.
 */
public interface SchemaChangeset extends MeshVertex {

	/**
	 * Returns the list of schema changes.
	 * 
	 * @return
	 */
	List<? extends SchemaChange> getChanges();

	/**
	 * Return the schema container which holds the schema upon which the stored changes build.
	 * 
	 * @return
	 */
	SchemaContainer getFromContainer();

	/**
	 * Return the schema container which holds the schema that was build by applying the stored changes.
	 * 
	 * @return
	 */
	SchemaContainer getToContainer();

	/**
	 * Add the given change to the changeset.
	 * 
	 * @param change
	 *            Change to be add
	 * @return Fluent API
	 */
	SchemaChangeset addChange(SchemaChange change);

}
