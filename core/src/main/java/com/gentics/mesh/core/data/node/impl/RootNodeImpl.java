package com.gentics.mesh.core.data.node.impl;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_PARENT_NODE;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

import java.util.List;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.GenericNode;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.generic.AbstractGenericNode;
import com.gentics.mesh.core.data.node.ContainerNode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.RootNode;
import com.gentics.mesh.core.data.service.transformation.TransformationParameters;
import com.gentics.mesh.core.rest.node.NodeResponse;

public class RootNodeImpl extends AbstractGenericNode<NodeResponse> implements RootNode {

	@Override
	public List<? extends Node> getChildren() {
		return in(HAS_PARENT_NODE).has(NodeImpl.class).toListExplicit(NodeImpl.class);
	}

	@Override
	public void setParentNode(ContainerNode parentNode) {
		throw new UnsupportedOperationException("The root node can't have any parent node.");
	}

	@Override
	public Node create() {
		Node node = BootstrapInitializer.getBoot().nodeRoot().create();
		node.setParentNode(this);
		return node;
	}

	@Override
	public GenericNode<NodeResponse> transformToRest(MeshAuthUser requestUser, Handler<AsyncResult<NodeResponse>> handler, TransformationParameters... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

}
