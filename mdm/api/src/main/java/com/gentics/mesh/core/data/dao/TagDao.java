package com.gentics.mesh.core.data.dao;

import java.util.List;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.root.RootDao;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * DAO for {@link HibTag}.
 */
public interface TagDao extends DaoGlobal<HibTag>, DaoTransformable<HibTag, TagResponse>, RootDao<HibTagFamily, HibTag> {

	/**
	 * Return the tag of the uuid.
	 * 
	 * @param project
	 * @param uuid
	 * @return
	 */
	HibTag findByUuid(HibProject project, String uuid);

	/**
	 * Return the sub etag of the tag.
	 * 
	 * @param tag
	 * @param ac
	 * @return
	 */
	String getSubETag(HibTag tag, InternalActionContext ac);

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

	/**
	 * Load the tag of the branch.
	 * 
	 * @param branch
	 * @param ac
	 * @param tagUuid
	 * @param perm
	 * @return
	 */
	HibTag loadObjectByUuid(HibBranch branch, InternalActionContext ac, String tagUuid, InternalPermission perm);

	/**
	 * Load the tag and check permissions.
	 * 
	 * @param project
	 * @param ac
	 * @param tagUuid
	 * @param perm
	 * @return
	 */
	HibTag loadObjectByUuid(HibProject project, InternalActionContext ac, String tagUuid, InternalPermission perm);

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
	Page<? extends HibNode> findTaggedNodes(HibTag tag, HibUser requestUser, HibBranch branch, List<String> languageTags,
		ContainerType type,
		PagingParameters pagingInfo);

	/**
	 * Return the tagged nodes.
	 * 
	 * @param tag
	 * @param ac
	 * @return
	 */
	Result<? extends HibNode> findTaggedNodes(HibTag tag, InternalActionContext ac);

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
	Result<? extends HibNode> getNodes(HibTag tag, HibBranch branch);

	/**
	 * Compute the total count of tags in the tag family.
	 * 
	 * @param tagFamily
	 * @return
	 */
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
	Result<HibTag> getTags(HibNode node, HibBranch branch);

	/**
	 * Return a page of all visible tags that are assigned to the node.
	 *
	 * @param user
	 * @param params
	 * @param branch
	 * @return Page which contains the result
	 */
	Page<? extends HibTag> getTags(HibNode node, HibUser user, PagingParameters params, HibBranch branch);

	/**
	 * Tests if the node is tagged with the given tag.
	 *
	 * @param tag
	 * @param branch
	 * @return
	 */
	boolean hasTag(HibNode node, HibTag tag, HibBranch branch);
}
