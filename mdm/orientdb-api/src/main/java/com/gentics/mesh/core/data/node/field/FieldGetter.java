package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.rest.schema.FieldSchema;

/**
 * Fetcher for fields from a content.
 */
@FunctionalInterface
public interface FieldGetter {

	/**
	 * Fetch the graph field from the given container which is identified using the given field schema.
	 * 
	 * @param container
	 * @param schema
	 * @return
	 */
	GraphField get(GraphFieldContainer container, FieldSchema schema);
}
