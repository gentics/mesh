package com.gentics.cailun.core.data.service;

import java.util.List;

import com.gentics.cailun.core.data.model.Content;
import com.gentics.cailun.core.data.model.Language;
import com.gentics.cailun.core.data.service.generic.GenericContentService;
import com.gentics.cailun.core.rest.content.response.ContentResponse;

public interface ContentService extends GenericContentService<Content> {

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
	public ContentResponse transformToRest(Content content, List<String> languageTags);

//	public Content save(String projectName, String path, ContentResponse requestModel);

}
