package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.root.RootDao;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;
import com.google.common.base.Predicate;

/**
 * DAO for tag family operations.
 */
public interface TagFamilyDao extends DaoGlobal<HibTagFamily>, DaoTransformable<HibTagFamily, TagFamilyResponse>, RootDao<HibProject, HibTagFamily> {

	/**
	 * Update the tag family.
	 * 
	 * @param tagFamily
	 *            Element to be updated
	 * @param ac
	 *            Context which provides update information
	 * @param batch
	 *            Batch to be used to collect events
	 * @return
	 */
	boolean update(HibTagFamily tagFamily, InternalActionContext ac, EventQueueBatch batch);

	// Find all tag families across all project.
	// TODO rename this method once ready
	Result<? extends HibTagFamily> findAllGlobal();

	/**
	 * Return a result for all tag families for the given project.
	 * 
	 * @param project
	 * @return
	 */
	Result<? extends HibTagFamily> findAll(HibProject project);

	/**
	 * Create a tag family
	 * 
	 * @param project
	 *            Project of the tag family
	 * @param ac
	 *            Context which contains the request information
	 * @param batch
	 *            Batch to be used
	 * @param uuid
	 *            UUID of the created element
	 * @return
	 */
	HibTagFamily create(HibProject project, InternalActionContext ac, EventQueueBatch batch, String uuid);

	/**
	 * Return the tag family with the given name.
	 * 
	 * @param name
	 * @return
	 */
	HibTagFamily findByName(String name);

	/**
	 * Return the tag family in the given project with the given name.
	 * 
	 * @param project
	 * @param name
	 * @return
	 */
	HibTagFamily findByName(HibProject project, String name);

	/**
	 * Find the tag family with given uuid (scoped to the given project)
	 * 
	 * @param project
	 * @param uuid
	 * @return
	 */
	HibTagFamily findByUuid(HibProject project, String uuid);

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
	 * Delete the tag family.
	 * 
	 * @param tagFamily
	 * @param bac
	 */
	void delete(HibTagFamily tagFamily, BulkActionContext bac);

	/**
	 * Return a page of tags.
	 * 
	 * @param tagFamily
	 * @param user
	 * @param pagingInfo
	 * @return
	 */
	Page<? extends HibTag> getTags(HibTagFamily tagFamily, HibUser user, PagingParameters pagingInfo);

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
	 * Load a page of tag families for the given project and paging info.
	 * 
	 * @param project
	 * @param ac
	 * @param pagingInfo
	 * @return
	 */
	Page<? extends HibTagFamily> findAll(HibProject project, InternalActionContext ac,
		PagingParameters pagingInfo);

	/**
	 * Load a page of tag families for the given project and paging info.
	 * 
	 * @param project
	 * @param ac
	 * @param pagingInfo
	 * @param filter
	 *            Filter to apply when loading the elements
	 * @return
	 */
	Page<? extends HibTagFamily> findAll(HibProject project, InternalActionContext ac, PagingParameters pagingInfo,
		Predicate<HibTagFamily> filter);

	/**
	 * Load the tag family by uuid.
	 * 
	 * @param project
	 * @param ac
	 * @param uuid
	 * @param perm
	 * @param errorIfNotFound
	 *            Throw an error when the element could not be found
	 * @return
	 */
	HibTagFamily loadObjectByUuid(HibProject project, InternalActionContext ac, String uuid, InternalPermission perm,
		boolean errorIfNotFound);

	/**
	 * Compute the total count of stored tag families.
	 * 
	 * @param project
	 * @return
	 */
	long computeCount(HibProject project);
}
