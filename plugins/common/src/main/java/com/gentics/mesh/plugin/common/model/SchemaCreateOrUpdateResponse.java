package com.gentics.mesh.plugin.common.model;

import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;

/**
 * Wrapper for {@link SchemaResponse} with a flag indicating whether the schema
 * was created or updated
 */
public class SchemaCreateOrUpdateResponse implements JsonSerializable {

	private final SchemaResponse schema;
	private final boolean updated;

	public SchemaCreateOrUpdateResponse(SchemaResponse schema, boolean updated) {
		this.schema = schema;
		this.updated = updated;
	}

	/**
	 * Get the wrapped schema response.
	 *
	 * @return The wrapped schema response
	 */
	public SchemaResponse getSchema() {
		return schema;
	}

	/**
	 * Indicates whether the wrapped schema was created or updated.
	 *
	 * @return <code>true</code> when the schema was updated, and
	 * 		<code>false</code> when it was newly created
	 */
	public boolean isUpdated() {
		return updated;
	}
}
