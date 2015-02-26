package com.gentics.cailun.core.data.service;

import com.gentics.cailun.core.data.model.Project;
import com.gentics.cailun.core.data.model.generic.GenericFile;
import com.gentics.cailun.core.data.service.generic.GenericNodeService;
import com.gentics.cailun.core.rest.response.RestProject;

public interface ProjectService extends GenericNodeService<Project> {

	Project findByName(String projectName);
	
	Project findByUUID(String uuid);

	RestProject getResponseObject(Project project);

	void deleteByName(String name);


}
