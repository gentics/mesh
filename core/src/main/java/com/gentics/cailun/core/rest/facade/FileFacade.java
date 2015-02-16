package com.gentics.cailun.core.rest.facade;

import com.gentics.cailun.core.rest.model.File;
import com.gentics.cailun.core.rest.model.Language;

public class FileFacade extends CaiLunNodeFacade {
	
	public void setFilename(Language language, String filename) {
		setI18NProperty(language, File.FILENAME_KEYWORD, filename);
	}
}
