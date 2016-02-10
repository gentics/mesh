package com.gentics.mesh.core.data.schema;

import java.io.IOException;
import java.util.List;

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;
import com.gentics.mesh.util.Tuple;

/**
 * A schema change represents a single manipulation of a field container (eg. Schema, Microschema).
 * 
 * <pre>
 * {@code
 *  (s:Schema)-[:HAS_CHANGE]->(c1:SchemaChange)-[:HAS_CHANGE]->(c2:SchemaChange)-(s2:Schema)
 * }
 * </pre>
 */
public interface SchemaChange<T extends FieldSchemaContainer> extends MeshVertex {

	/**
	 * Set the schema change operation.
	 * 
	 * @param action
	 * @return Fluent
	 */
	SchemaChange<T> setOperation(SchemaChangeOperation action);

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
	SchemaChange<?> getNextChange();

	/**
	 * Set the next change.
	 * 
	 * @param change
	 * @return
	 */
	SchemaChange<T> setNextChange(SchemaChange<?> change);

	/**
	 * Return the previous schema change.
	 * 
	 * @return
	 */
	SchemaChange<?> getPreviousChange();

	/**
	 * Set the previous change.
	 * 
	 * @param change
	 * @return
	 */
	SchemaChange<T> setPreviousChange(SchemaChange<?> change);

	/**
	 * Return the <b>in-bound</b> connected schema container.
	 * 
	 * @return
	 */
	GraphFieldSchemaContainer<?, ?, ?> getPreviousSchemaContainer();

	/**
	 * Set the <b>in-bound</b> connection from the schema change to the container.
	 * 
	 * @param container
	 * @return Fluent API
	 */
	SchemaChange<T> setPreviousContainer(GraphFieldSchemaContainer<?, ?, ?> container);

	/**
	 * Return the out-bound connected schema container.
	 * 
	 * @return
	 */
	GraphFieldSchemaContainer<?, ?, ?> getNextContainer();

	/**
	 * Set the out-bound connected schema container.
	 * 
	 * @param container
	 * @return
	 */
	SchemaChange<T> setNextSchemaContainer(GraphFieldSchemaContainer<?, ?, ?> container);

	/**
	 * Get the migration script for the change. May either be a custom script or an automatically created
	 * 
	 * @return migration script
	 * @throws IOException
	 */
	String getMigrationScript() throws IOException;

	/**
	 * Get the migration script context
	 * 
	 * @return context
	 */
	List<Tuple<String, Object>> getMigrationScriptContext();

	/**
	 * Get the automatic migration script (may be null)
	 * 
	 * @return automatic migration script
	 * @throws IOException
	 */
	String getAutoMigrationScript() throws IOException;

	/**
	 * Set a custom migration script. If this is set to null, the automatically created migration script will be used instead
	 *
	 * @param migrationScript
	 *            migration script
	 * @return fluent API
	 */
	SchemaChange<T> setCustomMigrationScript(String migrationScript);

	/**
	 * 
	 * Apply the current change on the field schema container (eg. {@link Schema} or {@link Microschema}).
	 * 
	 * @param container
	 *            Field container to be modified
	 * @return Modified schema
	 */
	T apply(T container);

	/**
	 * Set the change specific properties by examining the rest change model.
	 * 
	 * @param restChange
	 */
	void fill(SchemaChangeModel restChange);

}
