package com.gentics.mesh.core.data.service;

import io.vertx.ext.apex.RoutingContext;

import java.util.List;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.model.root.MeshRoot;
import com.gentics.mesh.core.data.model.tinkerpop.MeshNode;
import com.gentics.mesh.core.data.service.generic.GenericPropertyContainerService;
import com.gentics.mesh.core.rest.node.response.NodeResponse;
import com.gentics.mesh.paging.PagingInfo;

public interface MeshNodeService extends GenericPropertyContainerService<MeshNode> {

	/**
	 * Transforms the given node into a rest response. Only the specified languages will be included.
	 * 
	 * @param content
	 * @param languageTags
	 *            List of IETF language tags
	 * @return Rest response pojo
	 */
	public NodeResponse transformToRest(RoutingContext rc, MeshNode content);

	public Page<MeshNode> findAll(RoutingContext rc, String projectName, List<String> languageTags, PagingInfo pagingInfo);

	public Page<MeshNode> findChildren(RoutingContext rc, String projectName, MeshNode parentNode, List<String> languageTags, PagingInfo pagingInfo);

	public void createLink(MeshNode from, MeshNode to);

	public MeshNode create();

}
