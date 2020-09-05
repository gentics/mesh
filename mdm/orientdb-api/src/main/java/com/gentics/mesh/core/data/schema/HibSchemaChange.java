package com.gentics.mesh.core.data.schema;

import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;

public interface HibSchemaChange<T extends FieldSchemaContainer> extends HibBaseElement {

	HibSchemaChange<?> getNextChange();

	HibSchemaChange<T> setNextChange(HibSchemaChange<?> change);

	HibSchemaChange<?> getPreviousChange();

	<R extends FieldSchemaContainer> R apply(R container);

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
