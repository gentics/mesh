package com.gentics.cailun.core.data.service;

import java.util.ArrayList;
import java.util.List;

import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.data.model.Project;
import com.gentics.cailun.core.data.service.generic.GenericNodeServiceImpl;
import com.gentics.cailun.core.repository.ProjectRepository;
import com.gentics.cailun.core.rest.project.request.ProjectCreateRequest;
import com.gentics.cailun.core.rest.project.response.ProjectResponse;

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
	public List<Project> findAll() {
		// TODO i assume this could create memory problems for big data
		try (Transaction tx = springConfig.getGraphDatabaseService().beginTx()) {
			List<Project> list = new ArrayList<>();
			for (Project user : projectRepository.findAll()) {
				list.add(user);
			}
			tx.success();
			return list;
		}
	}

	@Override
	public void deleteByName(String name) {
		projectRepository.deleteByName(name);
	}

	@Override
	public Project transformFromRest(ProjectCreateRequest requestModel) {
		Project project = new Project(requestModel.getName());
		// TODO handle creator and roottag
		// project.setCreator(creator);
		return project;
	}

	@Override
	public ProjectResponse transformToRest(Project project) {
		ProjectResponse projectResponse = new ProjectResponse();
		projectResponse.setUuid(project.getUuid());
		projectResponse.setName(project.getName());
		return projectResponse;
	}

}
