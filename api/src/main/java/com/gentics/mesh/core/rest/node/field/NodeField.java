package com.gentics.mesh.core.rest.node.field;

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
	String getUrl();
}
