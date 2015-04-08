package com.gentics.cailun.core.data.service;

import io.vertx.ext.apex.RoutingContext;

import java.util.List;

import org.springframework.data.domain.Page;

import com.gentics.cailun.core.data.model.Content;
import com.gentics.cailun.core.data.model.Language;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.service.generic.GenericPropertyContainerService;
import com.gentics.cailun.core.rest.content.response.ContentResponse;
import com.gentics.cailun.path.PagingInfo;

public interface ContentService extends GenericPropertyContainerService<Content> {

	public void setTeaser(Content page, Language language, String text);

	public void setTitle(Content page, Language language, String text);

	/**
	 * Transforms the given content into a rest response. Only the specified languages will be included.
	 * 
	 * @param content
	 * @param languageTags
	 *            List of IETF language tags
	 * @return Rest response pojo
	 */
	public ContentResponse transformToRest(RoutingContext rc, Content content, List<String> languageTags, int depth);

	public Page<Content> findAllVisible(User requestUser, String projectName, List<String> languageTags, PagingInfo pagingInfo);

	public void createLink(Content from, Content to);

	public void addI18NContent(Content content, Language language, String text);

	public void setContent(Content content, Language language, String text);

	public void setFilename(Content content, Language language, String filename);
}
