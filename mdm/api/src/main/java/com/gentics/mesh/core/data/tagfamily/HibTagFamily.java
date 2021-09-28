package com.gentics.mesh.core.data.tagfamily;

import static com.gentics.mesh.util.URIUtils.encodeSegment;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibBucketableElement;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.user.HibUserTracking;
import com.gentics.mesh.core.rest.tag.TagFamilyReference;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.handler.VersionUtils;

/**
 * Domain model for tag families.
 */
public interface HibTagFamily extends HibCoreElement<TagFamilyResponse>, HibUserTracking, HibBucketableElement {

	/**
	 * Return the name.
	 * 
	 * @return
	 */
	String getName();

	/**
	 * Set the tag family name.
	 * 
	 * @param name
	 */
	void setName(String name);

	/**
	 * Return the project in which the tag family is used.
	 * 
	 * @return
	 */
	HibProject getProject();

	/**
	 * Delete the tag family.
	 */
	void deleteElement();

	/**
	 * Return the current element version.
	 * 
	 * TODO: Check how versions can be accessed via Hibernate and refactor / remove this method accordingly
	 * 
	 * @return
	 */
	String getElementVersion();

	/**
	 * Transform the tag family to a reference.
	 * 
	 * @return
	 */
	TagFamilyReference transformToReference();

	/**
	 * Return the description.
	 * 
	 * @return
	 */
	String getDescription();

	/**
	 * Set the description.
	 * 
	 * @param description
	 */
	void setDescription(String description);

	@Override
	default String getAPIPath(InternalActionContext ac) {
		return VersionUtils.baseRoute(ac) + "/" + encodeSegment(getProject().getName()) + "/tagFamilies/" + getUuid();
	}
}
