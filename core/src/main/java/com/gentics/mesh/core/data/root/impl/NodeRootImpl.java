package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.ASSIGNED_TO_PROJECT;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_NODE;
import static com.gentics.mesh.core.data.relationship.Permission.READ_PERM;

import java.util.List;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.root.NodeRoot;
import com.gentics.mesh.paging.PagingInfo;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.TraversalHelper;
import com.syncleus.ferma.traversals.VertexTraversal;

public class NodeRootImpl extends AbstractRootVertex<Node> implements NodeRoot {

	@Override
	protected Class<? extends Node> getPersistanceClass() {
		return NodeImpl.class;
	}

	@Override
	protected String getRootLabel() {
		return HAS_NODE;
	}

	@Override
	public Page<? extends Node> findAll(MeshAuthUser requestUser, String projectName, List<String> languageTags, PagingInfo pagingInfo)
			throws InvalidArgumentException {

		VertexTraversal<?, ?, ?> traversal = requestUser.getImpl().getPermTraversal(READ_PERM).has(NodeImpl.class).mark().out(ASSIGNED_TO_PROJECT)
				.has("name", projectName).back();
		VertexTraversal<?, ?, ?> countTraversal = requestUser.getImpl().getPermTraversal(READ_PERM).has(NodeImpl.class).mark()
				.out(ASSIGNED_TO_PROJECT).has("name", projectName).back();
		Page<? extends Node> nodePage = TraversalHelper.getPagedResult(traversal, countTraversal, pagingInfo, NodeImpl.class);
		return nodePage;
	}

}
