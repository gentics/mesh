package com.gentics.mesh.core.rest.node.field;

import java.util.Map;

import com.gentics.mesh.core.rest.node.FieldMap;

/**
 * A node field is a field which contains a node reference to other nodes.
 * Please note that {@link FieldMap} will handle the actual JSON format building.
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

	/**
	 * Return the language specific webroot paths to the node.
	 * 
	 * @return
	 */
	Map<String, String> getLanguagePaths();

	@Override
	default Object getValue() {
		return getUuid();
	}
}
