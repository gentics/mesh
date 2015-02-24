package com.gentics.cailun.core.repository.generic;

import com.gentics.cailun.core.data.model.generic.GenericFile;
import com.gentics.cailun.core.repository.action.GenericFileRepositoryActions;

public interface GenericFileRepository<T extends GenericFile> extends GenericPropertyContainerRepository<T>, GenericFileRepositoryActions<T> {

	public T findByProject(String projectName, String path);

}
