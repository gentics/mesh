package com.gentics.cailun.core.rest.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.rest.model.File;
import com.gentics.cailun.core.rest.model.Language;

@Component
@Transactional
public class FileServiceImpl extends CaiLunNodeServiceImpl implements FileService {

	public void setFilename(File file, Language language, String filename) throws UnsupportedOperationException {
		//TODO check for conflicting i18n filenames
		setI18NProperty(file, language, File.FILENAME_KEYWORD, filename);
	}
}
