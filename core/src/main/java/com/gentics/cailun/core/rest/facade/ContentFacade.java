package com.gentics.cailun.core.rest.facade;

import com.gentics.cailun.core.rest.model.Content;
import com.gentics.cailun.core.rest.model.File;
import com.gentics.cailun.core.rest.model.Language;

public class ContentFacade extends CaiLunNodeFacade {

	public ContentFacade(Language language, String name, String filename) {
		setI18NProperty(language, File.FILENAME_KEYWORD, filename);
		setI18NProperty(language, NAME_KEYWORD, name);
	}

	public void addI18Content(Language 	language, String content) {
		setI18NProperty(language, Content.CONTENT_KEYWORD, content);
	}
}
