package com.gentics.mesh.core.data.dao.impl;

import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;

import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.OrientDBBootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.dao.AbstractCoreDaoWrapper;
import com.gentics.mesh.core.data.dao.ProjectDao;
import com.gentics.mesh.core.data.dao.ProjectDaoWrapper;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.rest.event.project.ProjectMicroschemaEventModel;
import com.gentics.mesh.core.rest.event.project.ProjectSchemaEventModel;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.Assignment;
import com.gentics.mesh.parameter.PagingParameters;

import dagger.Lazy;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * DAO for project operations.
 * 
 * TODO MDM Use {@link ProjectDao} instead of ProjectRoot once ready
 */
@Singleton
public class ProjectDaoWrapperImpl extends AbstractCoreDaoWrapper<ProjectResponse, HibProject, Project> implements ProjectDaoWrapper {

	private static final Logger log = LoggerFactory.getLogger(ProjectDaoWrapperImpl.class);

	@Inject
	public ProjectDaoWrapperImpl(Lazy<OrientDBBootstrapInitializer> boot) {
		super(boot);
	}

	@Override
	public Page<? extends Project> findAll(InternalActionContext ac, PagingParameters pagingInfo) {
		return boot.get().meshRoot().getProjectRoot().findAll(ac, pagingInfo);
	}

	@Override
	public Page<? extends HibProject> findAll(InternalActionContext ac, PagingParameters pagingInfo, Predicate<HibProject> extraFilter) {
		return boot.get().meshRoot().getProjectRoot().findAllWrapped(ac, pagingInfo, extraFilter);
	}

	@Override
	public HibProject findByName(String name) {
		ProjectRoot root = boot.get().meshRoot().getProjectRoot();
		return root.findByName(name);
	}

	@Override
	public HibProject findByUuid(String uuid) {
		ProjectRoot root = boot.get().meshRoot().getProjectRoot();
		return root.findByUuid(uuid);
	}

	@Override
	public long count() {
		ProjectRoot projectRoot = boot.get().meshRoot().getProjectRoot();
		return projectRoot.globalCount();
	}

	@Override
	public ProjectSchemaEventModel onSchemaAssignEvent(HibProject project, HibSchema schema, Assignment assignment) {
		Project graphProject = toGraph(project);
		return graphProject.onSchemaAssignEvent(schema, assignment);
	}

	@Override
	public ProjectMicroschemaEventModel onMicroschemaAssignEvent(HibProject project, HibMicroschema microschema, Assignment assignment) {
		Project graphProject = toGraph(project);
		return graphProject.onMicroschemaAssignEvent(microschema, assignment);
	}

	@Override
	public String getSubETag(HibProject project, InternalActionContext ac) {
		return boot.get().meshRoot().getProjectRoot().getSubETag(toGraph(project), ac);
	}

	@Override
	public Result<? extends HibProject> findAll() {
		return boot.get().meshRoot().getProjectRoot().findAll();
	}

	@Override
	protected RootVertex<Project> getRoot() {
		return boot.get().meshRoot().getProjectRoot();
	}
}
