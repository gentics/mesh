package com.gentics.mesh.core.data.schema.handler;

import com.gentics.mesh.core.data.schema.HibFieldSchemaVersionElement;
import com.gentics.mesh.core.data.schema.HibSchemaChange;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainerVersion;
import com.gentics.mesh.core.rest.schema.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.SchemaModel;

/**
 * The field container mutator utilizes {@link SchemaChange} objects in order to modify/mutate a given field container implementation (e.g. {@link SchemaModel}
 * or {@link MicroschemaModel}. This way {@link SchemaChange} operations can be applied on a given schema.
 */
public class FieldSchemaContainerMutator {

	/**
	 * Applies all changes that are connected to the container to the version of the container and returns the mutated version of the field container that was
	 * initially loaded from the graph field container element.
	 * 
	 * @param containerVersion
	 *            Graph element that provides the chain of changes and the field container that should be mutated
	 * @return
	 */
	public <RM extends FieldSchemaContainerVersion> RM apply(HibFieldSchemaVersionElement<?, RM, ?, ?, ?> containerVersion) {
		RM oldSchema = containerVersion.getSchema();
		CommonTx.get().data().mesh().serverSchemaStorage().remove(oldSchema);
		HibSchemaChange<?> change = containerVersion.getNextChange();
		while (change != null) {
			oldSchema = change.apply(oldSchema);
			change = change.getNextChange();
		}
		return oldSchema;
	}

}
