package com.gentics.mesh.core.data.tag;

import com.gentics.mesh.core.data.HibBucketableElement;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.data.user.HibUserTracking;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.rest.tag.TagResponse;

/**
 * Domain model for tags.
 */
public interface HibTag extends HibCoreElement<TagResponse>, HibUserTracking, HibBucketableElement {

	/**
	 * Return the tag name.
	 * 
	 * @return
	 */
	String getName();

	/**
	 * Set the name.
	 * 
	 * @param name
	 */
	void setName(String name);

	/**
	 * Return the tag family of the tag.
	 * 
	 * @return
	 */
	HibTagFamily getTagFamily();

	/**
	 * Return the project in which the tag is used.
	 * 
	 * @return
	 */
	HibProject getProject();

	/**
	 * Delete the tag.
	 */
	void deleteElement();

	/**
	 * Transform the tag to a reference.
	 * 
	 * @return
	 */
	TagReference transformToReference();

	/**
	 * Return the current element version.
	 * 
	 * TODO: Check how versions can be accessed via Hibernate and refactor / remove this method accordingly
	 * 
	 * @return
	 */
	String getElementVersion();
}
