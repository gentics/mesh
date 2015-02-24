package com.gentics.cailun.core.data.service;

import java.util.List;

import com.gentics.cailun.core.data.model.Content;
import com.gentics.cailun.core.data.model.Language;
import com.gentics.cailun.core.data.service.generic.GenericContentService;
import com.gentics.cailun.core.rest.response.GenericContentResponse;

public interface ContentService extends GenericContentService<Content> {

	public void setTeaser(Content page, Language language, String text);

	public void setTitle(Content page, Language language, String text);

	public GenericContentResponse getReponseObject(Content content, List<String> languages);

}
