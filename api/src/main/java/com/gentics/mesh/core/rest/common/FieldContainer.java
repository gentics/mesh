package com.gentics.mesh.core.rest.common;

import com.gentics.mesh.core.rest.node.FieldMap;

/**
 * Interface for field containers
 */
public interface FieldContainer extends RestModel {

	/**
	 * Get the fields of the container
	 *
	 * @return
	 */
	FieldMap getFields();
}
