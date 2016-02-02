package com.gentics.mesh.core.data.schema;

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;

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
	 * Return the <b>in-bound</b> connected schema container.
	 * 
	 * @return
	 */
	SchemaContainer getPreviousSchemaContainer();

	/**
	 * Set the <b>in-bound</b> connection from the schema change to the container.
	 * 
	 * @param container
	 * @return Fluent API
	 */
	SchemaChange setPreviousSchemaContainer(SchemaContainer container);

	/**
	 * Return the out-bound connected schema container.
	 * 
	 * @return
	 */
	SchemaContainer getNextSchemaContainer();

	/**
	 * Set the out-bound connected schema container.
	 * 
	 * @param container
	 * @return
	 */
	SchemaChange setNextSchemaContainer(SchemaContainer container);

	/**
	 * Get the migration script for the change. May either be a custom script or an automatically created
	 * 
	 * @return migration script
	 */
	String getMigrationScript();

	/**
	 * Set a custom migration script. If this is set to null, the automatically created migration script will be used instead
	 *
	 * @param migrationScript
	 *            migration script
	 * @return fluent API
	 */
	SchemaChange setMigrationScript(String migrationScript);

	/**
	 * Apply the current change on the schema.
	 * 
	 * @param schema
	 *            Schema to be modified
	 * @return Modified schema
	 */
	Schema apply(Schema schema);
}
