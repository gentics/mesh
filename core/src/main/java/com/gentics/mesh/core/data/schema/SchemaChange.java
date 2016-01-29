package com.gentics.mesh.core.data.schema;

import com.gentics.mesh.core.data.MeshVertex;

/**
 * A schema change represents a single manipulation of a schema. Multiple {@link SchemaChange} objects are bundled together and form a {@link SchemaChangeset}.
 */
public interface SchemaChange extends MeshVertex {

	/**
	 * Return the changeset to which this change belongs.
	 * 
	 * @return
	 */
	SchemaChangeset getChangeset();

	/**
	 * Set the schema change action.
	 * 
	 * @param action
	 * @return Fluent
	 */
	SchemaChange setAction(SchemaChangeAction action);

	/**
	 * Set the field key for this change.
	 * 
	 * @param fieldKey
	 * @return
	 */
	SchemaChange setFieldKey(String fieldKey);

}
