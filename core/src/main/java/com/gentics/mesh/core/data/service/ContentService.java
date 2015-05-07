package com.gentics.mesh.core.data.service;

import io.vertx.ext.apex.RoutingContext;

import java.util.List;

import org.springframework.data.domain.Page;

import com.gentics.mesh.core.data.model.Content;
import com.gentics.mesh.core.data.model.auth.User;
import com.gentics.mesh.core.data.service.generic.GenericPropertyContainerService;
import com.gentics.mesh.core.rest.content.response.ContentResponse;
import com.gentics.mesh.paging.PagingInfo;

public interface ContentService extends GenericPropertyContainerService<Content> {

	/**
	 * Transforms the given content into a rest response. Only the specified languages will be included.
	 * 
	 * @param content
	 * @param languageTags
	 *            List of IETF language tags
	 * @return Rest response pojo
	 */
	public ContentResponse transformToRest(RoutingContext rc, Content content);

	public Page<Content> findAllVisible(User requestUser, String projectName, List<String> languageTags, PagingInfo pagingInfo);

	public void createLink(Content from, Content to);

}
