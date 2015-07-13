package com.gentics.mesh.core.data.root;

import java.util.List;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.util.InvalidArgumentException;

public interface NodeRoot extends RootVertex<Node, NodeResponse> {

	Page<? extends Node> findAll(MeshAuthUser requestUser, String projectName, List<String> languageTags, PagingInfo pagingInfo)
			throws InvalidArgumentException;

	Node create();

	void addNode(Node node);
	
	void removeNode(Node node);

}
