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
	 * Set the next change.
	 * 
	 * @param change
	 * @return
	 */
	SchemaChange setNextChange(SchemaChange change);

	/**
	 * Return the previous schema change.
	 * 
	 * @return
	 */
	SchemaChange getPreviousChange();

	/**
	 * Set the previous change.
	 * 
	 * @param change
	 * @return
	 */
	SchemaChange setPreviousChange(SchemaChange change);

	/**
	 * Return the in-bound connected schema container.
	 * 
	 * @return
	 */
	SchemaContainer getFromSchemaContainer();

	/**
	 * Set the in-bound connection from the schema change to the container.
	 * 
	 * @param container
	 * @return Fluent API
	 */
	SchemaChange setFromSchemaContainer(SchemaContainer container);

	/**
	 * Return the out-bound connected schema container.
	 * 
	 * @return
	 */
	SchemaContainer getToSchemaContainer();

	/**
	 * Set the out-bound connected schema container.
	 * 
	 * @param container
	 * @return
	 */
	SchemaChange setToSchemaContainer(SchemaContainer container);

}
