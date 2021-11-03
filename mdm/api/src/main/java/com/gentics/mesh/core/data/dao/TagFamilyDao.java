package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * DAO for tag family operations.
 */
public interface TagFamilyDao extends DaoGlobal<HibTagFamily>, DaoTransformable<HibTagFamily, TagFamilyResponse>, RootDao<HibProject, HibTagFamily> {

	/**
	 * Add the given tag to the tagfamily.
	 * 
	 * @param tagFamily
	 * @param tag
	 */
	void addTag(HibTagFamily tagFamily, HibTag tag);

	/**
	 * Remove the given tag from the tag family.
	 * 
	 * @param tagFamily
	 * @param tag
	 */
	void removeTag(HibTagFamily tagFamily, HibTag tag);

	/**
	 * Create the tag family.
	 * 
	 * @param project
	 *            Project for the tag family
	 * @param name
	 *            Name of the tag family
	 * @param user
	 *            Creator of the tag family
	 * @return
	 */
	HibTagFamily create(HibProject project, String name, HibUser user);

	/**
	 * Return a page of tags.
	 * 
	 * @param tagFamily
	 * @param user
	 * @param pagingInfo
	 * @return
	 */
	Page<? extends HibTag> getTags(HibTagFamily tagFamily, HibUser user, PagingParameters pagingInfo);
}
