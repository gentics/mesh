package com.gentics.mesh.core.verticle.handler;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.node.Node;

@FunctionalInterface
public interface TagListCallable {

	Page<? extends Tag> findTags(String projectName, Node node, PagingInfo pagingInfo);

}
