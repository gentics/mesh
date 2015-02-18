package com.gentics.cailun.core.rest.service.generic;

import com.gentics.cailun.core.rest.model.Language;
import com.gentics.cailun.core.rest.model.generic.GenericFile;

public interface GenericFileService<T extends GenericFile> extends GenericNodeService<T> {

	public void setFilename(T file, Language language, String filename) throws UnsupportedOperationException;

}
