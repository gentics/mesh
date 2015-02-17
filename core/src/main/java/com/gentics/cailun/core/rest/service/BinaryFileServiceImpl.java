package com.gentics.cailun.core.rest.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.rest.model.File;
import com.gentics.cailun.core.rest.model.Language;

@Component
@Transactional
public class BinaryFileServiceImpl extends FileServiceImpl implements BinaryFileService {

	/**
	 * I18N filenames are not supported for binary files.
	 * 
	 * @deprecated Use {@link #setFilename(String)} instead.
	 */
	@Override
	@Deprecated
	public void setFilename(File file, Language language, String filename) throws UnsupportedOperationException {
		throw new UnsupportedOperationException("I18N filenames are not supported for binary files");
	}
	
	

}
