package com.gentics.mesh.core.verticle.handler;

import java.util.List;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.paging.PagingInfo;

@FunctionalInterface
public interface NodeListTagCallable {

	Page<Node> findNodes(String projectName, Tag tag, List<String> languageTags, PagingInfo pagingInfo);

}
