package com.gentics.mesh.core.data.schema.handler;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.rest.schema.Schema;

/**
 * The schema mutator utilizes {@link SchemaChange} objects in order to modify/mutate a given schema. This way {@link SchemaChange} operations can be applied on
 * a given schema.
 */
@Component
public class SchemaMutator {

	/**
	 * Applies the given changes to the schema and returns the updated schema
	 * 
	 * @param schema
	 *            Schema to be updated
	 * @param changes
	 *            Changes that should be applied
	 * @return Schema with applied changes
	 */
	public Schema apply(Schema schema, SchemaChange... changes) {
		if (changes == null) {
			return schema;
		}

		// Iterate over all given changes and apply them in order specified
		for (SchemaChange change : changes) {
			schema = change.apply(schema);
		}
		return schema;
	}

}
