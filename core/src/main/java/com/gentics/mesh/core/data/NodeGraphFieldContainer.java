package com.gentics.mesh.core.data;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.rest.error.Errors;
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
	 * @return
	 */
	String getDisplayFieldValue();

	/**
	 * Get the parent node
	 *
	 * @return
	 */
	Node getParentNode();

	/**
	 * Update the property webroot path info. This will also check for
	 * uniqueness conflicts of the webroot path and will throw a
	 * {@link Errors#conflict(String, String, String, String...)} if one found
	 *
	 * @param conflictI18n
	 *            key of the message in case of conflicts
	 */
	void updateWebrootPathInfo(String conflictI18n);

	/**
	 * Return the schema container version that holds the schema that is used in combination with this node.
	 * 
	 * @return Schema container version
	 */
	SchemaContainerVersion getSchemaContainerVersion();

	/**
	 * Set the schema container version that is used in combination with this node.
	 * 
	 * @param schema
	 */
	void setSchemaContainerVersion(SchemaContainerVersion schema);
}
