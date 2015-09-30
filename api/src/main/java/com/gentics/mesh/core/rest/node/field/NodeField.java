package com.gentics.mesh.core.rest.node.field;

public interface NodeField extends ListableField, MicroschemaListableField {

	/**
	 * Return the uuid of the node.
	 * 
	 * @return
	 */
	String getUuid();

}
