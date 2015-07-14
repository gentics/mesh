package com.gentics.mesh.core.data.node.impl;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_PARENT_NODE;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

import java.util.List;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.GenericNode;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.AbstractGenericNode;
import com.gentics.mesh.core.data.node.BaseNode;
import com.gentics.mesh.core.data.node.ContainerNode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.node.NodeResponse;

public class BaseNodeImpl extends AbstractGenericNode<NodeResponse> implements BaseNode {

	@Override
	public List<? extends Node> getChildren() {
		return in(HAS_PARENT_NODE).has(NodeImpl.class).toListExplicit(NodeImpl.class);
	}

	@Override
	public void setParentNode(ContainerNode parentNode) {
		throw new UnsupportedOperationException("The root node can't have any parent node.");
	}

	@Override
	public Node create(User user, SchemaContainer container, Project project) {
		Node node = BootstrapInitializer.getBoot().nodeRoot().create(user, container, project);
		node.setParentNode(this);
		return node;
	}

	@Override
	public GenericNode<NodeResponse> transformToRest(RoutingContext rc, Handler<AsyncResult<NodeResponse>> handler) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void delete() {
		// TODO Auto-generated method stub

	}

}
