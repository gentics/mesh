package com.gentics.cailun.core.rest.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.rest.model.BinaryFile;
import com.gentics.cailun.core.rest.model.Language;
import com.gentics.cailun.core.rest.service.generic.GenericFileServiceImpl;

@Component
@Transactional
public class BinaryFileServiceImpl extends GenericFileServiceImpl<BinaryFile> implements BinaryFileService {

	/**
	 * I18N filenames are not supported for binary files.
	 * 
	 * @deprecated Use {@link #setFilename(String)} instead.
	 */
	@Override
	@Deprecated
	public void setFilename(BinaryFile file, Language language, String filename) throws UnsupportedOperationException {
		throw new UnsupportedOperationException("I18N filenames are not supported for binary files");
	}

}
