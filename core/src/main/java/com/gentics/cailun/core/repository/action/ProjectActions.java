package com.gentics.cailun.core.repository.action;

import com.gentics.cailun.core.rest.model.generic.GenericFile;

public interface ProjectActions {

	GenericFile findFileByPath(String projectName, String path);

}
