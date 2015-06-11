package com.gentics.mesh.core.data.service;

import io.vertx.ext.apex.RoutingContext;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.Result;
import com.gentics.mesh.core.data.model.root.ProjectRoot;
import com.gentics.mesh.core.data.model.tinkerpop.Project;
import com.gentics.mesh.core.data.model.tinkerpop.User;
import com.gentics.mesh.core.rest.project.response.ProjectResponse;
import com.gentics.mesh.paging.PagingInfo;

public interface ProjectService {

	Project findByName(String projectName);

	Project findByUUID(String uuid);

	Result<Project> findAll();

	void deleteByName(String name);

	ProjectResponse transformToRest(RoutingContext rc, Project project);

	Page<Project> findAllVisible(User requestUser, PagingInfo pagingInfo);

	Project create(String name);

	ProjectRoot createRoot();

	ProjectRoot findRoot();

	void delete(Project project);

}
