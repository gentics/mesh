package com.gentics.cailun.core.rest.service.generic;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.rest.model.Language;
import com.gentics.cailun.core.rest.model.generic.GenericFile;

@Component
@Transactional
public class GenericFileServiceImpl<T extends GenericFile> extends GenericNodeServiceImpl<T> implements GenericFileService<T> {

	public void setFilename(T file, Language language, String filename) throws UnsupportedOperationException {
		//TODO check for conflicting i18n filenames
		setI18NProperty(file, language, GenericFile.FILENAME_KEYWORD, filename);
	}
}
