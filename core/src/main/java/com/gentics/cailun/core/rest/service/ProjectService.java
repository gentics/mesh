package com.gentics.cailun.core.rest.service;

import com.gentics.cailun.core.rest.model.Project;
import com.gentics.cailun.core.rest.service.generic.GenericNodeService;

public interface ProjectService extends GenericNodeService<Project> {

	Project findByName(String projectName);

}
