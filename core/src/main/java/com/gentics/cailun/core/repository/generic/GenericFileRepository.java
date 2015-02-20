package com.gentics.cailun.core.repository.generic;

import com.gentics.cailun.core.repository.action.GenericFileRepositoryActions;
import com.gentics.cailun.core.rest.model.generic.GenericFile;

public interface GenericFileRepository<T extends GenericFile> extends GenericPropertyContainerRepository<T>, GenericFileRepositoryActions<T> {

	public T findByProject(String projectName, String path);

}
