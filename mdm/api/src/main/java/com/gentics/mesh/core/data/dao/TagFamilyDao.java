package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.project.Project;
import com.gentics.mesh.core.data.tag.Tag;
import com.gentics.mesh.core.data.tagfamily.TagFamily;
import com.gentics.mesh.core.data.user.User;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * DAO for tag family operations.
 */
public interface TagFamilyDao extends DaoGlobal<TagFamily>, DaoTransformable<TagFamily, TagFamilyResponse>, RootDao<Project, TagFamily> {

	/**
	 * Add the given tag to the tagfamily.
	 * 
	 * @param tagFamily
	 * @param tag
	 */
	void addTag(TagFamily tagFamily, Tag tag);

	/**
	 * Remove the given tag from the tag family.
	 * 
	 * @param tagFamily
	 * @param tag
	 */
	void removeTag(TagFamily tagFamily, Tag tag);

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
	TagFamily create(Project project, String name, User user);

	/**
	 * Create the tag family
	 * @param project Project for the tag family
	 * @param name Name of the tag family
	 * @param user Creator of the tag family
	 * @param uuid uuid of the tag family
	 * @return
	 */
	TagFamily create(Project project, String name, User user, String uuid);

	/**
	 * Return a page of tags.
	 * 
	 * @param tagFamily
	 * @param user
	 * @param pagingInfo
	 * @return
	 */
	Page<? extends Tag> getTags(TagFamily tagFamily, User user, PagingParameters pagingInfo);
}
