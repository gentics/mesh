package com.gentics.cailun.core.data.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.data.model.Project;
import com.gentics.cailun.core.data.service.generic.GenericNodeServiceImpl;
import com.gentics.cailun.core.repository.ProjectRepository;
import com.gentics.cailun.core.rest.response.RestProject;

@Component
@Transactional
public class ProjectServiceImpl extends GenericNodeServiceImpl<Project> implements ProjectService {

	@Autowired
	private ProjectRepository projectRepository;

	@Override
	public Project findByName(String projectName) {
		return projectRepository.findByName(projectName);
	}

	@Override
	public Project findByUUID(String uuid) {
		return projectRepository.findByUUID(uuid);
	}

	@Override
	public RestProject getResponseObject(Project project) {
		RestProject restProject = new RestProject();
		restProject.setUuid(project.getUuid());
		restProject.setName(project.getName());
		return restProject;
	}

}
