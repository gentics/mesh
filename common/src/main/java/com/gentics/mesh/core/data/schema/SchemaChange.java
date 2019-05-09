package com.gentics.mesh.core.data.schema;

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.rest.common.FieldContainer;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;
import io.vertx.core.json.JsonObject;

import java.io.IOException;
import java.util.Map;

/**
 * A schema change represents a single manipulation of a field container (e.g.: {@link Schema}, {@link Microschema}).
 * 
 * <pre>
 * {@code
 *  (s:SchemaVersion)-[:HAS_CHANGE]->(c1:SchemaChange)-[:HAS_CHANGE]->(c2:SchemaChange)-(s2:SchemaVersion)
 * }
 * </pre>
 * 
 * The schema change stores {@link SchemaChangeModel} data. Since the {@link SchemaChangeModel} class is generic we will also store the model specific
 * properties in a generic way. The {@link #setRestProperty(String, Object)} method can be used to set such properties.
 */
public interface SchemaChange<T extends FieldSchemaContainer> extends MeshVertex {

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
	 * Return the <b>in-bound</b> connected schema container version.
	 * 
	 * @return
	 */
	<R extends GraphFieldSchemaContainerVersion<?, ?, ?, ?, ?>> R getPreviousContainerVersion();

	/**
	 * Set the <b>in-bound</b> connection from the schema change to the container version.
	 * 
	 * @param containerVersion
	 * @return Fluent API
	 */
	SchemaChange<T> setPreviousContainerVersion(GraphFieldSchemaContainerVersion<?, ?, ?, ?, ?> containerVersion);

	/**
	 * Return the out-bound connected schema container version.
	 * 
	 * @return
	 */
	<R extends GraphFieldSchemaContainerVersion<?, ?, ?, ?, ?>> R getNextContainerVersion();

	/**
	 * Set the out-bound connected schema container.
	 * 
	 * @param containerVersion
	 * @return
	 */
	SchemaChange<T> setNextSchemaContainerVersion(GraphFieldSchemaContainerVersion<?, ?, ?, ?, ?> containerVersion);

	/**
	 *
	 * Apply the current change on the field schema container (eg. {@link Schema} or {@link Microschema}).
	 *
	 * @param container
	 *            Field container to be modified
	 * @return Modified schema
	 */
	<R extends FieldSchemaContainer> R apply(R container);

	/**
	 * Apply the current change on the field container to create a new field.
	 */
	Map<String, Field> createFields(FieldSchemaContainer oldSchema, FieldContainer oldContent);

	/**
	 * Set the change specific properties by examining the rest change model.
	 *
	 * @param restChange
	 */
	void updateFromRest(SchemaChangeModel restChange);

	/**
	 * Transform the graph model into the rest representation.
	 *
	 * @return
	 * @throws IOException
	 */
	SchemaChangeModel transformToRest() throws IOException;

	/**
	 * Set a REST specific property.
	 *
	 * @param key
	 * @param value
	 */
	void setRestProperty(String key, Object value);

	/**
	 * Return a REST specific property.
	 *
	 * @param key
	 * @return
	 */
	<R> R getRestProperty(String key);

	/**
	 * Return REST field specific properties.
	 *
	 * @return
	 */
	<R> Map<String, R> getRestProperties();

	/**
	 * Return the index options for this change. This can either hold index or field specific options.
	 *
	 * @return
	 */
	JsonObject getIndexOptions();

	/**
	 * Set the index options for the schema / field.
	 *
	 * @param options
	 */
	void setIndexOptions(JsonObject options);
}
