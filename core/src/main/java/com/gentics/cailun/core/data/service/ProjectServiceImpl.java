package com.gentics.cailun.core.data.service;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.data.model.Project;
import com.gentics.cailun.core.data.model.RootTag;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.service.generic.GenericNodeServiceImpl;
import com.gentics.cailun.core.repository.ProjectRepository;
import com.gentics.cailun.core.rest.project.request.ProjectCreateRequest;
import com.gentics.cailun.core.rest.project.response.ProjectResponse;
import com.gentics.cailun.paging.CaiLunPageRequest;
import com.gentics.cailun.paging.PagingInfo;

@Component
@Transactional(readOnly = true)
public class ProjectServiceImpl extends GenericNodeServiceImpl<Project> implements ProjectService {

	private static Logger log = LoggerFactory.getLogger(ProjectServiceImpl.class);

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
	public Result<Project> findAll() {
		return projectRepository.findAll();
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
		RootTag rootTag = neo4jTemplate.fetch(project.getRootTag());
		if (rootTag != null) {
			projectResponse.setRootTagUuid(rootTag.getUuid());
		} else {
			log.info("Inconsistency detected. Project {" + project.getUuid() + "} has no rootTag.");
		}
		return projectResponse;
	}

	@Override
	public Page<Project> findAllVisible(User requestUser, PagingInfo pagingInfo) {
		return projectRepository.findAll(requestUser, new CaiLunPageRequest(pagingInfo));
	}

}
