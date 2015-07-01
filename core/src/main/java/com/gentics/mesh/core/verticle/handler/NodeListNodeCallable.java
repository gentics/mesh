package com.gentics.mesh.core.verticle.handler;

import java.util.List;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.paging.PagingInfo;

@FunctionalInterface
public interface NodeListNodeCallable {

	Page<Node> findNodes(String projectName, Node parentNode, List<String> languageTags, PagingInfo pagingInfo);

}
