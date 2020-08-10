package com.gentics.mesh.core.data.search;

import com.gentics.mesh.core.data.search.context.GenericEntryContext;
import com.gentics.mesh.core.rest.schema.SchemaModel;

/**
 * Entry which instructs the index handler to create the index using the provided information.
 */
public interface CreateIndexEntry extends SeperateSearchQueueEntry<GenericEntryContext> {

	/**
	 * Return the configured schema for the index entry.
	 * 
	 * @return
	 */
	SchemaModel getSchema();

	/**
	 * Set the optional schema for the index create request.
	 * 
	 * @param schema
	 * @return Fluent API
	 */
	CreateIndexEntry setSchema(SchemaModel schema);

	/**
	 * Name of the index which should be created.
	 * 
	 * @return
	 */
	String getIndexName();

}
