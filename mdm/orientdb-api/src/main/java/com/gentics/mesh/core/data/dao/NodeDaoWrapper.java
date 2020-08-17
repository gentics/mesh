package com.gentics.mesh.core.data.dao;

import java.util.List;
import java.util.stream.Stream;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.PagingParameters;

public interface NodeDaoWrapper extends NodeDao, DaoWrapper<Node>, DaoTransformable<Node, NodeResponse> {

	/**
	 * Return the children for this node for all branches.
	 *
	 * @return
	 */
	TraversalResult<Node> getChildren(Node node);

	/**
	 * Return the children for this node in the given branch.
	 *
	 * @param branchUuid
	 * @return
	 */
	TraversalResult<Node> getChildren(Node node, String branchUuid);

	/**
	 * Return the children for this node. Only fetches nodes from the provided branch and also checks permissions.
	 */
	Stream<Node> getChildrenStream(Node node, InternalActionContext ac);

	/**
	 * Returns the parent node of this node.
	 *
	 * @param branchUuid
	 *            branch Uuid
	 * @return
	 */
	Node getParentNode(Node node, String branchUuid);

	/**
	 * Set the parent node of this node.
	 *
	 * @param branchUuid
	 * @param parentNode
	 */
	void setParentNode(Node node, String branchUuid, Node parentNode);

	/**
	 * Return a page with child nodes that are visible to the given user.
	 *
	 * @param ac
	 *            Context of the operation
	 * @param languageTags
	 * @param branchUuid
	 *            branch Uuid
	 * @param type
	 *            edge type
	 * @param pagingParameter
	 * @return
	 */
	TransformablePage<Node> getChildren(Node node, InternalActionContext ac, List<String> languageTags, String branchUuid, ContainerType type,
		PagingParameters pagingParameter);
}
