package com.gentics.mesh.core.data.schema.handler;

import java.util.List;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.data.schema.UpdateSchemaChange;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.Schema;

/**
 * The field container mutator utilizes {@link SchemaChange} objects in order to modify/mutate a given field container implementaton (eg. {@link Schema} or
 * {@link Microschema}. This way {@link SchemaChange} operations can be applied on a given schema.
 */
@Component
public class FieldSchemaContainerMutator {

	/**
	 * Applies the given changes to the schema and returns the updated schema.
	 * 
	 * @param container
	 *            Container to be updated
	 * @param changes
	 *            Changes that should be applied
	 * @return Container with applied changes
	 */
	public <T extends FieldSchemaContainer> T apply(T container, List<SchemaChange<T>> changes) {
		if (changes == null) {
			return container;
		}

		FieldSchemaContainer genericContainer = container;
		// Iterate over all given changes and apply them in order specified
		for (SchemaChange<T> change : changes) {
			genericContainer = change.apply(container);
		}
		return (T) genericContainer;
	}

}
