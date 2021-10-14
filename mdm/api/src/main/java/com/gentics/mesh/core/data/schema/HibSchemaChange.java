package com.gentics.mesh.core.data.schema;

import java.io.IOException;
import java.util.Map;

import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.rest.common.FieldContainer;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;

import io.vertx.core.json.JsonObject;

/**
 * A schema change tracks the changes that are listed in between two schema versions.
 * 
 * @param <T>
 */
public interface HibSchemaChange<T extends FieldSchemaContainer> extends HibBaseElement {

	/**
	 * Load the next change.
	 * 
	 * @return Next change or null when no futher changes exist
	 */
	HibSchemaChange<?> getNextChange();

	/**
	 * Set the next change that should follow up on the current change.
	 * 
	 * @param change
	 * @return
	 */
	HibSchemaChange<T> setNextChange(HibSchemaChange<?> change);

	/**
	 * Return the previous change.
	 * 
	 * @return Previous change or null when no previous change could be found
	 */
	HibSchemaChange<?> getPreviousChange();

	/**
	 * Set the next change that should follow up on the current change.
	 * 
	 * @param change
	 * @return
	 */
	HibSchemaChange<T> setPreviousChange(HibSchemaChange<?> change);

	/**
	 * Apply the change onto the given REST model.
	 * 
	 * @param <R>
	 * @param container
	 * @return
	 */
	<R extends FieldSchemaContainer> R apply(R container);

	/**
	 * Return the next container version.
	 * 
	 * @param <R>
	 * @return
	 */
	<R extends HibFieldSchemaVersionElement<?, ?, ?, ?, ?>> R getNextContainerVersion();

	/**
	 * Return the <b>in-bound</b> connected schema container version.
	 * 
	 * @return
	 */
	<R extends HibFieldSchemaVersionElement<?, ?, ?, ?, ?>> R getPreviousContainerVersion();

	/**
	 * Set the <b>in-bound</b> connection from the schema change to the container version.
	 * 
	 * @param containerVersion
	 * @return Fluent API
	 */
	HibSchemaChange<T> setPreviousContainerVersion(HibFieldSchemaVersionElement<?, ?, ?, ?, ?> containerVersion);

	/**
	 * Set the out-bound connected schema container.
	 * 
	 * @param containerVersion
	 * @return
	 */
	HibSchemaChange<T> setNextSchemaContainerVersion(HibFieldSchemaVersionElement<?, ?, ?, ?, ?> containerVersion);

	/**
	 * Return the schema change operation.
	 * 
	 * @return
	 */
	SchemaChangeOperation getOperation();

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
