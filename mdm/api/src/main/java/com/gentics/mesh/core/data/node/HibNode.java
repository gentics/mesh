package com.gentics.mesh.core.data.node;

import java.util.List;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibBucketableElement;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.user.HibCreatorTracking;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.event.EventQueueBatch;

/**
 * Domain model for nodes.
 */
public interface HibNode extends HibCoreElement, HibCreatorTracking, HibBucketableElement {

	/**
	 * Return the element version string.
	 * 
	 * @return
	 */
	String getElementVersion();

	/**
	 * Transform the node to a reference POJO.
	 * 
	 * @param ac
	 * @return
	 */
	NodeReference transformToReference(InternalActionContext ac);

	/**
	 * Update the tags for the node.
	 * 
	 * @param ac
	 * @param batch
	 * @param tags
	 */
	void updateTags(InternalActionContext ac, EventQueueBatch batch, List<TagReference> tags);

	/**
	 * Return the project of the node.
	 * 
	 * @return
	 */
	HibProject getProject();

	/**
	 * Return the schema of the node.
	 * 
	 * @return
	 */
	// TODO rename method
	HibSchema getSchemaContainer();

	/**
	 * Set the schema for the node.
	 * 
	 * @param container
	 */
	void setSchemaContainer(HibSchema container);

	/**
	 * Remove the element.
	 */
	void removeElement();

}
