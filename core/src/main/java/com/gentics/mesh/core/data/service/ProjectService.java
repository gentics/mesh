package com.gentics.mesh.core.data.service;

import io.vertx.ext.apex.RoutingContext;

import org.springframework.data.domain.Page;
import org.springframework.data.neo4j.conversion.Result;

import com.gentics.mesh.core.data.model.Project;
import com.gentics.mesh.core.data.model.auth.User;
import com.gentics.mesh.core.data.service.generic.GenericNodeService;
import com.gentics.mesh.core.rest.project.response.ProjectResponse;
import com.gentics.mesh.paging.PagingInfo;

public interface ProjectService extends GenericNodeService<Project> {

	Project findByName(String projectName);

	Project findByUUID(String uuid);

	Result<Project> findAll();

	void deleteByName(String name);

	ProjectResponse transformToRest(RoutingContext rc, Project project);

	Page<Project> findAllVisible(User requestUser, PagingInfo pagingInfo);

}
