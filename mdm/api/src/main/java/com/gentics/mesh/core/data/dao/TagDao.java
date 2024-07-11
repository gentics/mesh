package com.gentics.mesh.core.data.dao;

import java.util.List;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.branch.Branch;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.Project;
import com.gentics.mesh.core.data.tag.Tag;
import com.gentics.mesh.core.data.tagfamily.TagFamily;
import com.gentics.mesh.core.data.user.User;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * DAO for {@link Tag}.
 */
public interface TagDao extends DaoGlobal<Tag>, DaoTransformable<Tag, TagResponse>, RootDao<TagFamily, Tag> {

	/**
	 * Return the tag of the uuid.
	 * 
	 * @param project
	 * @param uuid
	 * @return
	 */
	Tag findByUuid(Project project, String uuid);

	/**
	 * Return the sub etag of the tag.
	 * 
	 * @param tag
	 * @param ac
	 * @return
	 */
	String getSubETag(Tag tag, InternalActionContext ac);

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
	Tag create(TagFamily tagFamily, String name, Project project, User creator);

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
	Tag create(TagFamily tagFamily, String name, Project project, User creator, String uuid);

	/**
	 * Load the tag of the branch.
	 * 
	 * @param branch
	 * @param ac
	 * @param tagUuid
	 * @param perm
	 * @return
	 */
	Tag loadObjectByUuid(Branch branch, InternalActionContext ac, String tagUuid, InternalPermission perm);

	/**
	 * Load the tag and check permissions.
	 * 
	 * @param project
	 * @param ac
	 * @param tagUuid
	 * @param perm
	 * @return
	 */
	Tag loadObjectByUuid(Project project, InternalActionContext ac, String tagUuid, InternalPermission perm);

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
	Page<? extends Node> findTaggedNodes(Tag tag, User requestUser, Branch branch, List<String> languageTags,
		ContainerType type,
		PagingParameters pagingInfo);

	/**
	 * Return the tagged nodes.
	 * This will also check for the required permission
	 * @param tag tag
	 * @param ac action context
	 * @param perm permission to check
	 * @return result
	 */
	Result<? extends Node> findTaggedNodes(Tag tag, InternalActionContext ac, InternalPermission perm);

	/**
	 * Unassign the the node from the tag.
	 *
	 * @param node
	 */
	void removeNode(Tag tag, Node node);

	/**
	 * Return a traversal result of nodes that were tagged by this tag in the given branch
	 *
	 * @param branch
	 * @return Result
	 */
	Result<? extends Node> getNodes(Tag tag, Branch branch);

	/**
	 * Add the given tag to the list of tags for this node in the given branch.
	 *
	 * @param tag
	 * @param branch
	 */
	void addTag(Node node, Tag tag, Branch branch);

	/**
	 * Remove the given tag from the list of tags for this node in the given branch.
	 *
	 * @param tag
	 * @param branch
	 */
	void removeTag(Node node, Tag tag, Branch branch);

	/**
	 * Remove all tags for the given branch.
	 *
	 * @param branch
	 */
	void removeAllTags(Node node, Branch branch);

	/**
	 * Return a list of all tags that were assigned to this node in the given branch.
	 *
	 * @param branch
	 * @return
	 */
	Result<Tag> getTags(Node node, Branch branch);

	/**
	 * Return a page of all visible tags that are assigned to the node.
	 *
	 * @param user
	 * @param params
	 * @param branch
	 * @return Page which contains the result
	 */
	Page<? extends Tag> getTags(Node node, User user, PagingParameters params, Branch branch);

	/**
	 * Tests if the node is tagged with the given tag.
	 *
	 * @param tag
	 * @param branch
	 * @return
	 */
	boolean hasTag(Node node, Tag tag, Branch branch);
}
