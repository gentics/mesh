package com.gentics.mesh.core.data.schema;

import com.gentics.mesh.core.data.MeshVertex;

/**
 * A schema change represents a single manipulation of a schema.
 * 
 * <pre>
 * {@code
 *  (s:Schema)-[:HAS_CHANGE]->(c1:SchemaChange)-[:HAS_CHANGE]->(c2:SchemaChange)-(s2:Schema)
 * }
 * </pre>
 */
public interface SchemaChange extends MeshVertex {

	/**
	 * Set the schema change operation.
	 * 
	 * @param action
	 * @return Fluent
	 */
	SchemaChange setOperation(SchemaChangeOperation action);

	/**
	 * Return the schema change operation.
	 * 
	 * @return
	 */
	SchemaChangeOperation getOperation();

	/**
	 * Set the field key for this change.
	 * 
	 * @param fieldKey
	 * @return
	 */
	SchemaChange setFieldKey(String fieldKey);

	/**
	 * Return the next schema change.
	 * 
	 * @return
	 */
	SchemaChange getNextChange();

	/**
	 * Return the previous schema change.
	 * 
	 * @return
	 */
	SchemaChange getPreviousChange();

	/**
	 * Return the schema container to which the schema change belongs.
	 * 
	 * @return
	 */
	SchemaContainer getOldSchemaContainer();

	/**
	 * Return the schema container that was build using this schema change.
	 * 
	 * @return
	 */
	SchemaContainer getNewSchemaContainer();

}
