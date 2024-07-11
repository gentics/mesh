package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.data.Field;
import com.gentics.mesh.core.data.FieldContainer;
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
	Field get(FieldContainer container, FieldSchema schema);
}
