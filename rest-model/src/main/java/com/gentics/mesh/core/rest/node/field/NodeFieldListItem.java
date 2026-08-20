package com.gentics.mesh.core.rest.node.field;

import com.gentics.mesh.core.rest.common.RestModel;

/**
 * Entry for a node list REST model.
 */
public interface NodeFieldListItem extends RestModel {

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
