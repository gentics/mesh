package com.gentics.mesh.core.data.schema;

import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaReference;

/**
 * A schema container is a graph element which stores the JSON schema data.
 */
public interface SchemaContainer extends GraphFieldSchemaContainer<Schema, SchemaReference, SchemaContainer, SchemaContainerVersion> {

	public static final String TYPE = "schemaContainer";


	

}
