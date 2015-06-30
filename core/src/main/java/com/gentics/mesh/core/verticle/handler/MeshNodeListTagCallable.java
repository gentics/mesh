package com.gentics.mesh.core.verticle.handler;

import java.util.List;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.node.MeshNode;
import com.gentics.mesh.paging.PagingInfo;

@FunctionalInterface
public interface MeshNodeListTagCallable {

	Page<MeshNode> findNodes(String projectName, Tag tag, List<String> languageTags, PagingInfo pagingInfo);

}
