package com.gentics.mesh.core.data.schema;

import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;

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
	<R extends HibFieldSchemaVersionElement<?, ?, ?, ?>> R getNextContainerVersion();

	/**
	 * Return the <b>in-bound</b> connected schema container version.
	 * 
	 * @return
	 */
	<R extends HibFieldSchemaVersionElement<?, ?, ?, ?>> R getPreviousContainerVersion();

	/**
	 * Set the <b>in-bound</b> connection from the schema change to the container version.
	 * 
	 * @param containerVersion
	 * @return Fluent API
	 */
	HibSchemaChange<T> setPreviousContainerVersion(HibFieldSchemaVersionElement<?, ?, ?, ?> containerVersion);

	/**
	 * Set the out-bound connected schema container.
	 * 
	 * @param containerVersion
	 * @return
	 */
	HibSchemaChange<T> setNextSchemaContainerVersion(HibFieldSchemaVersionElement<?, ?, ?, ?> containerVersion);

}
