package com.gentics.mesh.core.data;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.error.Errors;

/**
 * A node field container is a aggregation node that holds localized fields.
 *
 */
public interface NodeGraphFieldContainer extends GraphFieldContainer {

	/**
	 * Delete the field container. This will also delete linked elements like lists
	 */
	void delete(SearchQueueBatch batch);

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
	 * Update the property webroot path info. This will also check for uniqueness conflicts of the webroot path and will throw a
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

	/**
	 * Add a search queue batch entry to the given batch for the given action.
	 * 
	 * @param batch
	 * @param action
	 */
	void addIndexBatchEntry(SearchQueueBatch batch, SearchQueueEntryAction action);
}
