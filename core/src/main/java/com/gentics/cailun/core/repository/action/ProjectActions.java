package com.gentics.cailun.core.repository.action;

import com.gentics.cailun.core.data.model.generic.GenericFile;

public interface ProjectActions {

	GenericFile findFileByPath(String projectName, String path);

}
