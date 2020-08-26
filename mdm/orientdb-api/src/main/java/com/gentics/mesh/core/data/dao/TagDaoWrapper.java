package com.gentics.mesh.core.data.dao;

import java.util.List;
import java.util.function.Predicate;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.PagingParameters;

public interface TagDaoWrapper extends TagDao, DaoWrapper<HibTag>, DaoTransformable<HibTag, TagResponse> {

	/**
	 * Find all tags of the given tagfamily.
	 * 
	 * @param tagFamily
	 * @return
	 */
	TraversalResult<? extends HibTag> findAll(HibTagFamily tagFamily);

	HibTag findByUuid(HibProject project, String uuid);

	HibTag findByUuid(HibTagFamily tagFamily, String uuid);

	HibTag findByName(String name);

	HibTag findByName(HibTagFamily tagFamily, String name);

	Page<? extends HibTag> findAll(HibTagFamily tagFamily, InternalActionContext ac, PagingParameters pagingParameters);

	Page<? extends HibTag> findAll(HibTagFamily tagFamily, InternalActionContext ac, PagingParameters pagingInfo, Predicate<HibTag> extraFilter);

	String getSubETag(HibTag tag, InternalActionContext ac);

	HibTag create(HibTagFamily tagFamily, InternalActionContext ac, EventQueueBatch batch);

	HibTag create(HibTagFamily tagFamily, InternalActionContext ac, EventQueueBatch batch, String uuid);

	/**
	 * Create a new tag with the given name and creator. Note that this method will not check for any tag name collisions. Note that the created tag will also
	 * be assigned to the global root vertex.
	 *
	 * @param name
	 *            Name of the new tag.
	 * @param project
	 *            Root project of the tag.
	 * @param creator
	 *            User that is used to assign creator and editor references of the new tag.
	 * @return
	 */
	HibTag create(HibTagFamily tagFamily, String name, HibProject project, HibUser creator);

	/**
	 * Create a new tag with the given name and creator. Note that this method will not check for any tag name collisions. Note that the created tag will also
	 * be assigned to the global root vertex.
	 *
	 * @param name
	 *            Name of the new tag.
	 * @param project
	 *            Root project of the tag.
	 * @param creator
	 *            User that is used to assign creator and editor references of the new tag.
	 * @param uuid
	 *            Optional uuid
	 * @return
	 */
	HibTag create(HibTagFamily tagFamily, String name, HibProject project, HibUser creator, String uuid);

	void delete(HibTag tag, BulkActionContext bac);

	boolean update(HibTag tag, InternalActionContext ac, EventQueueBatch batch);

	String getETag(HibTag tag, InternalActionContext ac);

	String getAPIPath(HibTag tag, InternalActionContext ac);

	HibTag loadObjectByUuid(HibBranch branch, InternalActionContext ac, String tagUuid, InternalPermission perm);

	TraversalResult<? extends HibTag> findAllGlobal();

	HibTag loadObjectByUuid(HibProject project, InternalActionContext ac, String tagUuid, InternalPermission readPerm);

	/**
	 * Return a page of nodes that are visible to the user and which are tagged by this tag. Use the paging and language information provided.
	 *
	 * @param tag
	 * @param requestUser
	 * @param branch
	 * @param languageTags
	 * @param type
	 * @param pagingInfo
	 * @return
	 */
	TransformablePage<? extends HibNode> findTaggedNodes(HibTag tag, HibUser requestUser, HibBranch branch, List<String> languageTags,
		ContainerType type,
		PagingParameters pagingInfo);

	TraversalResult<? extends HibNode> findTaggedNodes(HibTag tag, InternalActionContext ac);

	/**
	 * Unassign the the node from the tag.
	 *
	 * @param node
	 */
	void removeNode(HibTag tag, HibNode node);

	/**
	 * Return a traversal result of nodes that were tagged by this tag in the given branch
	 *
	 * @param branch
	 * @return Result
	 */
	TraversalResult<? extends HibNode> getNodes(HibTag tag, HibBranch branch);

	long computeCount(HibTagFamily tagFamily);

	/**
	 * Add the given tag to the list of tags for this node in the given branch.
	 *
	 * @param tag
	 * @param branch
	 */
	void addTag(HibNode node, HibTag tag, HibBranch branch);

	/**
	 * Remove the given tag from the list of tags for this node in the given branch.
	 *
	 * @param tag
	 * @param branch
	 */
	void removeTag(HibNode node, HibTag tag, HibBranch branch);

	/**
	 * Remove all tags for the given branch.
	 *
	 * @param branch
	 */
	void removeAllTags(HibNode node, HibBranch branch);

	/**
	 * Return a list of all tags that were assigned to this node in the given branch.
	 *
	 * @param branch
	 * @return
	 */
	TraversalResult<HibTag> getTags(HibNode node, HibBranch branch);

	/**
	 * Return a page of all visible tags that are assigned to the node.
	 *
	 * @param user
	 * @param params
	 * @param branch
	 * @return Page which contains the result
	 */
	TransformablePage<? extends HibTag> getTags(HibNode node, HibUser user, PagingParameters params, HibBranch branch);

	/**
	 * Tests if the node is tagged with the given tag.
	 *
	 * @param tag
	 * @param branch
	 * @return
	 */
	boolean hasTag(HibNode node, HibTag tag, HibBranch branch);
}
