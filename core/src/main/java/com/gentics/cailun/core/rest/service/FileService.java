package com.gentics.cailun.core.rest.service;

import com.gentics.cailun.core.rest.model.File;
import com.gentics.cailun.core.rest.model.Language;

public interface FileService extends CaiLunNodeService {

	public void setFilename(File file, Language language, String filename) throws UnsupportedOperationException;
}
