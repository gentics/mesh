package com.gentics.cailun.core.repository.action;

import com.gentics.cailun.core.rest.model.generic.GenericFile;

public interface GenericFileRepositoryActions<T extends GenericFile> {

	public GenericFile findByProjectPath(String projectName, String path);

}
