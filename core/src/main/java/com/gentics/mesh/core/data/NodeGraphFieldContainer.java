package com.gentics.mesh.core.data;

import com.gentics.mesh.core.rest.schema.Schema;

/**
 * A node field container is a aggregation node that holds localized fields.
 *
 */
public interface NodeGraphFieldContainer extends GraphFieldContainer {

	/**
	 * Delete the field container. This will also delete linked elements like lists
	 */
	void delete();

	/**
	 * Return the display field value for this container.
	 * 
	 * @param schema
	 * @return
	 */
	String getDisplayFieldValue(Schema schema);

}
