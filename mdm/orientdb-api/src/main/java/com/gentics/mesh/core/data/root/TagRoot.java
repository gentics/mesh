package com.gentics.mesh.core.data.root;

import java.util.List;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * Aggregation node for tags.
 */
public interface TagRoot extends RootVertex<Tag>, TransformableElementRoot<Tag, TagResponse> {

	/**
	 * Add the given tag to the aggregation vertex.
	 * 
	 * @param tag
	 *            Tag to be added
	 */
	void addTag(HibTag tag);

	/**
	 * Remove the tag from the aggregation vertex.
	 * 
	 * @param tag
	 *            Tag to be removed
	 */
	void removeTag(HibTag tag);

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
	Page<? extends Node> findTaggedNodes(Tag tag, HibUser requestUser, Branch branch, List<String> languageTags, ContainerType type,
		PagingParameters pagingInfo);

	/**
	 * Load all nodes which have been tagged by the tag.
	 * 
	 * @param tag
	 * @param ac
	 * @return
	 */
	Result<? extends Node> findTaggedNodes(HibTag tag, InternalActionContext ac);

	/**
	 * Return a traversal result of nodes that were tagged by this tag in the given branch
	 *
	 * @param branch
	 *            branch
	 *
	 * @return Result
	 */
	Result<? extends Node> getNodes(Tag tag, HibBranch branch);
}