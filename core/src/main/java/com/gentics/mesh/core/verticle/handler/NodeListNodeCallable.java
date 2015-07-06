package com.gentics.mesh.core.verticle.handler;

import java.util.List;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.node.Node;

@FunctionalInterface
public interface NodeListNodeCallable {

	Page<Node> findNodes(String projectName, Node parentNode, List<String> languageTags, PagingInfo pagingInfo);

}
