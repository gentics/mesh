package com.gentics.mesh.core.data.schema;

import java.io.IOException;
import java.util.Map;

import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.rest.common.FieldContainer;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;

import io.vertx.core.json.JsonObject;

/**
 * A schema change tracks the changes that are listed in between two schema versions,
 * representing a single manipulation of a field container (e.g.: {@link SchemaModel}, {@link MicroschemaModel}).
 * 
 * <pre>
 * {@code
 *  (s:SchemaVersion)-[:HAS_CHANGE]->(c1:SchemaChange)-[:HAS_CHANGE]->(c2:SchemaChange)-(s2:SchemaVersion)
 * }
 * </pre>
 * 
 * The schema change stores {@link SchemaChangeModel} data. Since the {@link SchemaChangeModel} class is generic we will also store the model specific
 * properties in a generic way. The {@link #setRestProperty(String, Object)} method can be used to set such properties.
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

	/**
	 * Transform the entity model into the rest representation.
	 *
	 * @return
	 * @throws IOException
	 */
	default SchemaChangeModel transformToRest() throws IOException {
		SchemaChangeModel model = new SchemaChangeModel();
		model.getProperties().putAll(getRestProperties());
		model.setOperation(getOperation());
		model.setUuid(getUuid());
		return model;
	}

	/**
	 * Set the change specific properties by examining the rest change model.
	 *
	 * @param restChange
	 */
	default void updateFromRest(SchemaChangeModel restChange) {
		for (String key : restChange.getProperties().keySet()) {
			setRestProperty(key, restChange.getProperties().get(key));
		}
	}
}
