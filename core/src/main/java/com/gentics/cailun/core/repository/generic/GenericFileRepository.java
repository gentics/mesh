package com.gentics.cailun.core.repository.generic;

import com.gentics.cailun.core.rest.model.generic.GenericFile;

public interface GenericFileRepository<T extends GenericFile> extends GenericPropertyContainerRepository<T> {

	public T findByProject(String projectName, String path);

}
