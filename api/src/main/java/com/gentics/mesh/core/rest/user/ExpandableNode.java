package com.gentics.mesh.core.rest.user;

/**
 * Marker interface which is used to identify a node field which can be expanded.
 */
public interface ExpandableNode {

	/**
	 * Return the node uuid.
	 * 
	 * @return Uuid
	 */
	String getUuid();
}
