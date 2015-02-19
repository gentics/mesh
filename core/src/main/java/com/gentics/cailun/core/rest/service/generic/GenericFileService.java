package com.gentics.cailun.core.rest.service.generic;

import com.gentics.cailun.core.rest.model.Language;
import com.gentics.cailun.core.rest.model.generic.GenericFile;

public interface GenericFileService<T extends GenericFile> extends GenericPropertyContainerService<T> {

	public void setFilename(T file, Language language, String filename) throws UnsupportedOperationException;

	public T findByProject(String projectName, String path);

}
