package com.gentics.mesh.core.rest.node.field;

/**
 * Entry for a node list REST model.
 */
public interface NodeFieldListItem {

	/**
	 * Return the item node uuid.
	 * 
	 * @return Uuid of the node item
	 */
	String getUuid();

	/**
	 * Get the webroot URL to the node
	 * 
	 * @return webroot URL
	 */
	String getPath();

}
