package com.gentics.mesh.core.rest.user;

import com.gentics.mesh.core.rest.common.RestModel;

/**
 * Marker interface which is used to identify a node field which can be expanded.
 */
public interface ExpandableNode extends RestModel {

	/**
	 * Return the node uuid.
	 * 
	 * @return Uuid
	 */
	String getUuid();
}
