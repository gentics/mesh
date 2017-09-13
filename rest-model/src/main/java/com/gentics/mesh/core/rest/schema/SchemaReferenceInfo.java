package com.gentics.mesh.core.rest.schema;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;

public class SchemaReferenceInfo {

	@JsonDeserialize(as = SchemaReferenceImpl.class)
	private SchemaReference schema;

	/**
	 * Return the schema reference.
	 * 
	 * @return Schema reference
	 */
	public SchemaReference getSchema() {
		return schema;
	}

	/**
	 * Set the schema reference.
	 * 
	 * @param schema
	 *            Schema reference
	 * @return Fluent API
	 */
	public SchemaReferenceInfo setSchema(SchemaReference schema) {
		this.schema = schema;
		return this;
	}
}
