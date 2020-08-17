package com.gentics.mesh.core.data.dao.impl;

import java.util.List;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.dao.NodeDaoWrapper;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.PagingParameters;

import dagger.Lazy;

@Singleton
public class NodeDaoWrapperImpl implements NodeDaoWrapper {
	private final Lazy<BootstrapInitializer> boot;

	@Inject
	public NodeDaoWrapperImpl(Lazy<BootstrapInitializer> boot) {
		this.boot = boot;
	}

	@Override
	public Node findByUuidGlobal(String uuid) {
		// TODO Probably wrong interface hierarchy. There is no need for this method
		throw new RuntimeException("Not implemented");
	}

	@Override
	public long computeGlobalCount() {
		// TODO Probably wrong interface hierarchy. There is no need for this method
		throw new RuntimeException("Not implemented");
	}

	@Override
	public NodeResponse transformToRestSync(Node element, InternalActionContext ac, int level, String... languageTags) {
		return element.transformToRestSync(ac, level, languageTags);
	}


	@Override
	public TraversalResult<Node> getChildren(Node node) {
		return node.getChildren();
	}

	@Override
	public TraversalResult<Node> getChildren(Node node, String branchUuid) {
		return node.getChildren(branchUuid);
	}

	@Override
	public Stream<Node> getChildrenStream(Node node, InternalActionContext ac) {
		return node.getChildrenStream(ac);
	}

	@Override
	public Node getParentNode(Node node, String branchUuid) {
		return node.getParentNode(branchUuid);
	}

	@Override
	public void setParentNode(Node node, String branchUuid, Node parentNode) {
		node.setParentNode(branchUuid, parentNode);
	}

	@Override
	public TransformablePage<Node> getChildren(Node node, InternalActionContext ac, List<String> languageTags, String branchUuid, ContainerType type, PagingParameters pagingParameter) {
		return node.getChildren(ac, languageTags, branchUuid, type, pagingParameter);
	}
}
