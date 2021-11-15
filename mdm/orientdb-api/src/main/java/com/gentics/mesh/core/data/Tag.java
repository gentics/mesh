package com.gentics.mesh.core.data;

import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.search.GraphDBBucketableElement;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.rest.tag.TagResponse;

/**
 * Graph domain model interface for a tag.
 * 
 * Tags can currently only hold a single string value. Tags are not localizable. A tag can only be assigned to a single tag family.
 */
public interface Tag
	extends MeshCoreVertex<TagResponse>, ReferenceableElement<TagReference>, UserTrackingVertex, ProjectElement, HibTag, GraphDBBucketableElement {

	/**
	 * Return the tag family to which the tag belongs.
	 * 
	 * @return Tag family of the tag
	 */
	HibTagFamily getTagFamily();

	/**
	 * Set the tag family of this tag.
	 * 
	 * @param tagFamily
	 */
	void setTagFamily(HibTagFamily tagFamily);

	/**
	 * Return the project to which the tag was assigned to
	 * 
	 * @return Project of the tag
	 */
	HibProject getProject();

	/**
	 * Set the project to the tag.
	 * 
	 * @param project
	 */
	void setProject(HibProject project);

}
