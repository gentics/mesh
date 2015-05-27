package com.gentics.mesh.core.verticle.handler;

import java.util.List;

import org.springframework.data.domain.Page;

import com.gentics.mesh.core.data.model.MeshNode;
import com.gentics.mesh.core.data.model.Tag;
import com.gentics.mesh.paging.PagingInfo;

@FunctionalInterface
public interface MeshNodeListTagCallable {

	Page<MeshNode> findNodes(String projectName, Tag tag, List<String> languageTags, PagingInfo pagingInfo);

}
