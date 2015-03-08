package com.gentics.cailun.core.data.service;

import java.util.List;

import com.gentics.cailun.core.data.model.Content;
import com.gentics.cailun.core.data.model.Language;
import com.gentics.cailun.core.data.service.generic.GenericContentService;
import com.gentics.cailun.core.rest.content.response.ContentResponse;

public interface ContentService extends GenericContentService<Content> {

	public void setTeaser(Content page, Language language, String text);

	public void setTitle(Content page, Language language, String text);

	public ContentResponse getReponseObject(Content content, List<String> languages);

	public Content save(String projectName, String path, ContentResponse requestModel);

}
