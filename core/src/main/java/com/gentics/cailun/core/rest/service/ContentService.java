package com.gentics.cailun.core.rest.service;

import com.gentics.cailun.core.rest.model.Content;
import com.gentics.cailun.core.rest.model.Language;

public interface ContentService extends FileService {

	public void createLink(Content from, Content to);

	public void addI18Content(Content content, Language language, String text);

}
