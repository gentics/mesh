package com.gentics.mesh.core.rest.node.field;

/**
 * A node field is a field which contains a node reference to other nodes.
 */
public interface NodeField extends ListableField, MicroschemaListableField {

	/**
	 * Return the uuid of the node.
	 * 
	 * @return Uuid of the node
	 */
	String getUuid();

	/**
	 * Get the webroot URL to the node
	 * 
	 * @return webroot URL
	 */
	String getPath();
}
