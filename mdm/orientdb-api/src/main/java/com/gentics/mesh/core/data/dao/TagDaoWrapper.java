package com.gentics.mesh.core.data.dao;

import java.util.List;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.PagingParameters;

public interface TagDaoWrapper extends TagDao, DaoTransformable<Tag, TagResponse> {

	/**
	 * Find all tags of the given tagfamily.
	 * 
	 * @param tagFamily
	 * @return
	 */
	TraversalResult<? extends Tag> findAll(TagFamily tagFamily);

	Tag findByName(TagFamily tagFamily, String name);

	String getSubETag(Tag tag, InternalActionContext ac);

	void delete(Tag tag, BulkActionContext bac);

	boolean update(Tag tag, InternalActionContext ac, EventQueueBatch batch);

	String getETag(Tag tag, InternalActionContext ac);

	String getAPIPath(Tag tag, InternalActionContext ac);

	Tag loadObjectByUuid(Branch branch, InternalActionContext ac, String tagUuid, GraphPermission perm);

	TraversalResult<? extends Tag> findAllGlobal();

	Tag loadObjectByUuid(Project project, InternalActionContext ac, String tagUuid, GraphPermission readPerm);

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
	TransformablePage<? extends Node> findTaggedNodes(Tag tag, MeshAuthUser requestUser, Branch branch, List<String> languageTags, ContainerType type,
		PagingParameters pagingInfo);

	TraversalResult<? extends Node> findTaggedNodes(Tag tag, InternalActionContext ac);

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
	 *            branch
	 *
	 * @return Result
	 */
	TraversalResult<? extends Node> getNodes(Tag tag, Branch branch);
}
