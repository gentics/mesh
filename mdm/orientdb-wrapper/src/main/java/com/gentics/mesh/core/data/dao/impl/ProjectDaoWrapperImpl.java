package com.gentics.mesh.core.data.dao.impl;

import static com.gentics.mesh.core.data.perm.InternalPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;
import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;

import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.AbstractDaoWrapper;
import com.gentics.mesh.core.data.dao.NodeDaoWrapper;
import com.gentics.mesh.core.data.dao.ProjectDao;
import com.gentics.mesh.core.data.dao.ProjectDaoWrapper;
import com.gentics.mesh.core.data.dao.SchemaDaoWrapper;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;
import com.gentics.mesh.core.data.generic.PermissionPropertiesImpl;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.error.NameConflictException;
import com.gentics.mesh.core.rest.event.MeshEventModel;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.project.ProjectUpdateRequest;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.Assignment;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.parameter.value.FieldsSet;
import com.gentics.mesh.router.RouterStorageRegistry;

import dagger.Lazy;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * DAO for project operations.
 * 
 * TODO MDM Use {@link ProjectDao} instead of ProjectRoot once ready
 */
@Singleton
public class ProjectDaoWrapperImpl extends AbstractDaoWrapper<HibProject> implements ProjectDaoWrapper {

	private static final Logger log = LoggerFactory.getLogger(ProjectDaoWrapperImpl.class);

	private final RouterStorageRegistry routerStorageRegistry;

	@Inject
	public ProjectDaoWrapperImpl(Lazy<BootstrapInitializer> boot, Lazy<PermissionPropertiesImpl> permissions,
		RouterStorageRegistry routerStorageRegistry) {
		super(boot, permissions);
		this.routerStorageRegistry = routerStorageRegistry;
	}

	@Override
	public boolean update(HibProject project, InternalActionContext ac, EventQueueBatch batch) {
		ProjectUpdateRequest requestModel = ac.fromJson(ProjectUpdateRequest.class);

		String oldName = project.getName();
		String newName = requestModel.getName();
		routerStorageRegistry.assertProjectName(newName);
		if (shouldUpdate(newName, oldName)) {
			// Check for conflicting project name
			Project projectWithSameName = boot.get().projectRoot().findByName(newName);
			if (projectWithSameName != null && !projectWithSameName.getUuid().equals(project.getUuid())) {
				throw conflict(projectWithSameName.getUuid(), newName, "project_conflicting_name");
			}

			project.setName(newName);
			project.setEditor(ac.getUser());
			project.setLastEditedTimestamp();

			// Update the project and its nodes in the index
			batch.add(project.onUpdated());
			return true;
		}
		return false;
	}

	@Override
	public Result<? extends HibProject> findAll() {
		return boot.get().projectRoot().findAll();
	}

	@Override
	public Page<? extends Project> findAll(InternalActionContext ac, PagingParameters pagingInfo) {
		return boot.get().projectRoot().findAll(ac, pagingInfo);
	}

	@Override
	public Page<? extends HibProject> findAll(InternalActionContext ac, PagingParameters pagingInfo, Predicate<HibProject> extraFilter) {
		return boot.get().projectRoot().findAllWrapped(ac, pagingInfo, extraFilter);
	}

	@Override
	public HibProject findByName(String name) {
		ProjectRoot root = boot.get().projectRoot();
		return root.findByName(name);
	}

	@Override
	public HibProject findByName(InternalActionContext ac, String projectName, InternalPermission perm) {
		ProjectRoot root = boot.get().projectRoot();
		return root.findByName(ac, projectName, perm);
	}

	@Override
	public HibProject findByUuid(String uuid) {
		ProjectRoot root = boot.get().projectRoot();
		return root.findByUuid(uuid);
	}

	@Override
	public HibProject findByUuidGlobal(String uuid) {
		return findByUuid(uuid);
	}

	@Override
	public HibProject loadObjectByUuid(InternalActionContext ac, String uuid, InternalPermission perm) {
		ProjectRoot root = boot.get().projectRoot();
		return root.loadObjectByUuid(ac, uuid, perm);
	}

	@Override
	public Project loadObjectByUuid(InternalActionContext ac, String uuid, InternalPermission perm, boolean errorIfNotFound) {
		ProjectRoot root = boot.get().projectRoot();
		return root.loadObjectByUuid(ac, uuid, perm, errorIfNotFound);
	}

	@Override
	public HibProject create(String name, String hostname, Boolean ssl, String pathPrefix, HibUser creator, HibSchemaVersion schemaVersion,
		String uuid, EventQueueBatch batch) {
		SchemaVersion graphSchemaVersion = toGraph(schemaVersion);
		ProjectRoot root = boot.get().projectRoot();

		Project project = root.create();
		if (uuid != null) {
			project.setUuid(uuid);
		}
		project.setName(name);
		project.getNodeRoot();

		// Create the initial branch for the project and add the used schema
		// version to it
		Branch branch = project.getBranchRoot().create(name, creator, batch);
		branch.setMigrated(true);
		if (hostname != null) {
			branch.setHostname(hostname);
		}
		if (ssl != null) {
			branch.setSsl(ssl);
		}
		if (pathPrefix != null) {
			branch.setPathPrefix(pathPrefix);
		} else {
			branch.setPathPrefix("");
		}
		branch.assignSchemaVersion(creator, schemaVersion, batch);

		// Assign the provided schema container to the project
		project.getSchemaContainerRoot().addItem(toGraph(graphSchemaVersion.getSchemaContainer()));
		// project.getLatestBranch().assignSchemaVersion(creator,
		// schemaContainerVersion);
		project.createBaseNode(creator, graphSchemaVersion);

		project.setCreated(creator);
		project.setEditor(creator);
		project.getSchemaContainerRoot();
		project.getTagFamilyRoot();

		root.addItem(project);

		return project;
	}

	@Override
	public HibProject create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		ProjectRoot projectRoot = boot.get().projectRoot();
		UserDaoWrapper userDao = boot.get().userDao();
		SchemaDaoWrapper schemaDao = boot.get().schemaDao();

		// TODO also create a default object schema for the project. Move this
		// into service class
		// ObjectSchema defaultContentSchema = objectSchemaRoot.findByName(,
		// name)
		ProjectCreateRequest requestModel = ac.fromJson(ProjectCreateRequest.class);
		String projectName = requestModel.getName();
		HibUser creator = ac.getUser();

		if (StringUtils.isEmpty(requestModel.getName())) {
			throw error(BAD_REQUEST, "project_missing_name");
		}
		if (!userDao.hasPermission(creator, projectRoot, CREATE_PERM)) {
			throw error(FORBIDDEN, "error_missing_perm", projectRoot.getUuid(), CREATE_PERM.getRestPerm().getName());
		}
		// TODO instead of this check, a constraint in the db should be added
		HibProject conflictingProject = projectRoot.findByName(requestModel.getName());
		if (conflictingProject != null) {
			throw new NameConflictException("project_conflicting_name", projectName, conflictingProject.getUuid());
		}
		routerStorageRegistry.assertProjectName(requestModel.getName());

		if (requestModel.getSchema() == null || !requestModel.getSchema().isSet()) {
			throw error(BAD_REQUEST, "project_error_no_schema_reference");
		}
		HibSchemaVersion schemaVersion = schemaDao.fromReference(requestModel.getSchema());

		String hostname = requestModel.getHostname();
		Boolean ssl = requestModel.getSsl();
		String pathPrefix = requestModel.getPathPrefix();
		HibProject project = create(projectName, hostname, ssl, pathPrefix, creator, schemaVersion, uuid, batch);
		HibBranch initialBranch = project.getInitialBranch();
		String branchUuid = initialBranch.getUuid();

		// Add project permissions
		userDao.addCRUDPermissionOnRole(creator, projectRoot, CREATE_PERM, toGraph(project));
		userDao.inheritRolePermissions(creator, project, project.getBaseNode());
		userDao.inheritRolePermissions(creator, project, toGraph(project).getTagFamilyRoot());
		userDao.inheritRolePermissions(creator, project, toGraph(project).getSchemaContainerRoot());
		userDao.inheritRolePermissions(creator, project, toGraph(project).getMicroschemaContainerRoot());
		userDao.inheritRolePermissions(creator, project, toGraph(project).getNodeRoot());
		userDao.inheritRolePermissions(creator, project, initialBranch);

		// Store the project and the branch in the index
		batch.add(project.onCreated());
		batch.add(initialBranch.onCreated());

		// Add events for created basenode
		batch.add(project.getBaseNode().onCreated());
		toGraph(project.getBaseNode()).getDraftGraphFieldContainers().forEach(c -> {
			batch.add(c.onCreated(branchUuid, DRAFT));
		});

		return project;

	}

	@Override
	public void delete(HibProject project, BulkActionContext bac) {
		if (log.isDebugEnabled()) {
			log.debug("Deleting project {" + project.getName() + "}");
		}
		NodeDaoWrapper nodeDao = boot.get().nodeDao();

		Project graphProject = toGraph(project);

		// Remove the nodes in the project hierarchy
		HibNode base = graphProject.getBaseNode();
		nodeDao.delete(base, bac, true, true);

		// Remove the tagfamilies from the index
		graphProject.getTagFamilyRoot().delete(bac);

		// Remove all nodes in this project
		for (Node node : graphProject.findNodes()) {
			nodeDao.delete(node, bac, true, false);
			bac.inc();
		}

		// Finally also remove the node root
		graphProject.getNodeRoot().delete(bac);

		// Unassign the schema from the container
		for (Schema container : graphProject.getSchemaContainerRoot().findAll()) {
			graphProject.getSchemaContainerRoot().removeSchemaContainer(container, bac.batch());
		}

		// Remove the project schema root from the index
		graphProject.getSchemaContainerRoot().delete(bac);

		// Remove the branch root and all branches
		graphProject.getBranchRoot().delete(bac);

		// Remove the project from the index
		bac.add(graphProject.onDeleted());

		// Finally remove the project node
		graphProject.getVertex().remove();

		bac.process(true);

	}

	@Override
	public ProjectResponse transformToRestSync(HibProject project, InternalActionContext ac, int level, String... languageTags) {
		Project graphProject = toGraph(project);

		GenericParameters generic = ac.getGenericParameters();
		FieldsSet fields = generic.getFields();

		ProjectResponse restProject = new ProjectResponse();
		if (fields.has("name")) {
			restProject.setName(graphProject.getName());
		}
		if (fields.has("rootNode")) {
			restProject.setRootNode(graphProject.getBaseNode().transformToReference(ac));
		}

		graphProject.fillCommonRestFields(ac, fields, restProject);
		setRolePermissions(graphProject, ac, restProject);

		return restProject;

	}

	@Override
	public long globalCount() {
		ProjectRoot projectRoot = boot.get().projectRoot();
		return projectRoot.globalCount();
	}

	@Override
	public String getETag(HibProject project, InternalActionContext ac) {
		Project graphProject = toGraph(project);
		return graphProject.getETag(ac);
	}

	@Override
	public String getAPIPath(HibProject project, InternalActionContext ac) {
		Project graphProject = toGraph(project);
		return graphProject.getAPIPath(ac);
	}

	@Override
	public MeshEventModel onSchemaAssignEvent(HibProject project, HibSchema schema, Assignment assignment) {
		Project graphProject = toGraph(project);
		return graphProject.onSchemaAssignEvent(schema, assignment);
	}

}
