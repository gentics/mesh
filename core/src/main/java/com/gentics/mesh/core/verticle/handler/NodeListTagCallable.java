package com.gentics.mesh.core.verticle.handler;

import java.util.List;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.node.Node;

@FunctionalInterface
public interface NodeListTagCallable {

	Page<Node> findNodes(String projectName, Tag tag, List<String> languageTags, PagingInfo pagingInfo);

}
