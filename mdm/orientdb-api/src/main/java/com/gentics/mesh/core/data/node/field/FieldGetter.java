package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.data.HibField;
import com.gentics.mesh.core.data.HibFieldContainer;
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
	HibField get(HibFieldContainer container, FieldSchema schema);
}
