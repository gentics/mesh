package com.gentics.cailun.core.data.service;

import com.gentics.cailun.core.data.model.Project;
import com.gentics.cailun.core.data.service.generic.GenericNodeService;

public interface ProjectService extends GenericNodeService<Project> {

	Project findByName(String projectName);

}
