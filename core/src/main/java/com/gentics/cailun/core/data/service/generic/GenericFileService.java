package com.gentics.cailun.core.data.service.generic;

import com.gentics.cailun.core.data.model.Language;
import com.gentics.cailun.core.data.model.generic.GenericFile;

public interface GenericFileService<T extends GenericFile> extends GenericPropertyContainerService<T> {

	public void setFilename(T file, Language language, String filename) throws UnsupportedOperationException;

	public T findByProjectPath(String projectName, String path);

}
