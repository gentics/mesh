package com.gentics.mesh.core.data.service;

import io.vertx.ext.apex.RoutingContext;

import java.util.List;

import org.springframework.data.domain.Page;

import com.gentics.mesh.core.data.model.MeshNode;
import com.gentics.mesh.core.data.model.auth.User;
import com.gentics.mesh.core.data.service.generic.GenericPropertyContainerService;
import com.gentics.mesh.core.rest.meshnode.response.MeshNodeResponse;
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
	public MeshNodeResponse transformToRest(RoutingContext rc, MeshNode content);

	public Page<MeshNode> findAllVisible(User requestUser, String projectName, List<String> languageTags, PagingInfo pagingInfo);

	public void createLink(MeshNode from, MeshNode to);

}
