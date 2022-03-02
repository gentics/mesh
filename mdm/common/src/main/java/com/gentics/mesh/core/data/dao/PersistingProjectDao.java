package com.gentics.mesh.core.data.dao;

import static com.gentics.mesh.core.data.perm.InternalPermission.CREATE_PERM;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_MICROSCHEMA_ASSIGNED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_MICROSCHEMA_UNASSIGNED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_SCHEMA_ASSIGNED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_SCHEMA_UNASSIGNED;
import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import javax.naming.InvalidNameException;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.HibLanguage;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.error.NameConflictException;
import com.gentics.mesh.core.rest.event.project.ProjectMicroschemaEventModel;
import com.gentics.mesh.core.rest.event.project.ProjectSchemaEventModel;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.project.ProjectUpdateRequest;
import com.gentics.mesh.event.Assignment;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.value.FieldsSet;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * A persisting extension to {@link ProjectDao}
 * 
 * @author plyhun
 *
 */
public interface PersistingProjectDao extends ProjectDao, PersistingDaoGlobal<HibProject> {
	static final Logger log = LoggerFactory.getLogger(ProjectDao.class);

	/**
	 * Return the schema container permission root for the project.
	 * 
	 * @return
	 */
	HibBaseElement getSchemaContainerPermissionRoot(HibProject project);

	/**
	 * Return the microschema container permission root for the project.
	 *
	 * @return
	 */
	HibBaseElement getMicroschemaContainerPermissionRoot(HibProject project);

	/**
	 * Return the node permission root of the project. Internally this method will create the root when it has not yet been created.
	 * 
	 * @return Node root element
	 */
	HibBaseElement getNodePermissionRoot(HibProject project);

	@Override
	default HibProject findByName(InternalActionContext ac, String projectName, InternalPermission perm) {
		HibProject project = findByName(projectName);
		if (project == null) {
			throw error(NOT_FOUND, "object_not_found_for_name", projectName);
		}
		return checkPerms(project, project.getUuid(), ac, perm, true);
	}

	@Override
	default HibProject create(String projectName, String hostname, Boolean ssl, String pathPrefix, HibUser creator,
		HibSchemaVersion schemaVersion, EventQueueBatch batch) {
		return create(projectName, hostname, ssl, pathPrefix, creator, schemaVersion, null, batch);
	}

	@Override
	default ProjectResponse transformToRestSync(HibProject project, InternalActionContext ac, int level, String... languageTags) {
		GenericParameters generic = ac.getGenericParameters();
		FieldsSet fields = generic.getFields();

		ProjectResponse restProject = new ProjectResponse();
		if (fields.has("name")) {
			restProject.setName(project.getName());
		}
		if (fields.has("rootNode")) {
			restProject.setRootNode(Tx.get().nodeDao().transformToReference(project.getBaseNode(), ac));
		}

		project.fillCommonRestFields(ac, fields, restProject);
		Tx.get().roleDao().setRolePermissions(project, ac, restProject);

		return restProject;
	}

	@Override
	default String getSubETag(HibProject project, InternalActionContext ac) {
		return String.valueOf(project.getLastEditedTimestamp());
	}

	@Override
	default HibProject create(String name, String hostname, Boolean ssl, String pathPrefix, HibUser creator, HibSchemaVersion schemaVersion,
		String uuid, EventQueueBatch batch) {
		PersistingBranchDao branchDao = CommonTx.get().branchDao();
		SchemaDao schemaDao = Tx.get().schemaDao();
		
		HibProject project = createPersisted(uuid);
		project.setName(name);

		// triggering node permission root creation
		getNodePermissionRoot(project);

		// Create the initial branch for the project and add the used schema version to it
		HibBranch branch = branchDao.create(project, name, creator, batch);
		
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
		branchDao.assignSchemaVersion(branch, creator, schemaVersion, batch);

		// Assign the provided schema container to the project
		schemaDao.addItem(project, schemaDao.findByUuid(schemaVersion.getSchemaContainer().getUuid()));
		createBaseNode(project, creator, schemaVersion);

		project.setCreated(creator);
		project.setEditor(creator);

		// triggering permission roots creation
		getSchemaContainerPermissionRoot(project);
		getTagFamilyPermissionRoot(project);

		//root.addItem(project);

		return project;
	}

	@Override
	default HibNode createBaseNode(HibProject project, HibUser creator, HibSchemaVersion schemaVersion) {
		HibNode baseNode = project.getBaseNode();
		CommonTx ctx = CommonTx.get();
		ContentDao contentDao = ctx.contentDao();
		if (baseNode == null) {
			baseNode = ctx.create(CommonTx.get().nodeDao().getPersistenceClass(project));
			baseNode.setSchemaContainer(schemaVersion.getSchemaContainer());
			baseNode.setProject(project);
			baseNode.setCreated(creator);
			HibLanguage language = ctx.languageDao().findByLanguageTag(ctx.data().options().getDefaultLanguage());
			contentDao.createFieldContainer(baseNode, language.getLanguageTag(), project.getLatestBranch(), creator);
			project.setBaseNode(baseNode);
		}
		return baseNode;
	}

	@Override
	default HibProject create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		HibBaseElement projectPermRoot = CommonTx.get().data().permissionRoots().project();
		UserDao userDao = CommonTx.get().userDao();
		SchemaDao schemaDao = CommonTx.get().schemaDao();
		ContentDao contentDao = CommonTx.get().contentDao();

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
		if (!userDao.hasPermission(creator, projectPermRoot, CREATE_PERM)) {
			throw error(FORBIDDEN, "error_missing_perm", projectPermRoot.getUuid(), CREATE_PERM.getRestPerm().getName());
		}
		// TODO instead of this check, a constraint in the db should be added
		HibProject conflictingProject = findByName(requestModel.getName());
		if (conflictingProject != null) {
			throw new NameConflictException("project_conflicting_name", projectName, conflictingProject.getUuid());
		}
		CommonTx.get().data().mesh().routerStorageRegistry().assertProjectName(requestModel.getName());

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
		userDao.addCRUDPermissionOnRole(creator, projectPermRoot, CREATE_PERM, project);
		userDao.inheritRolePermissions(creator, project, project.getBaseNode());
		userDao.inheritRolePermissions(creator, project, getTagFamilyPermissionRoot(project));
		userDao.inheritRolePermissions(creator, project, getSchemaContainerPermissionRoot(project));
		userDao.inheritRolePermissions(creator, project, getMicroschemaContainerPermissionRoot(project));
		userDao.inheritRolePermissions(creator, project, getNodePermissionRoot(project));
		userDao.inheritRolePermissions(creator, project, initialBranch);

		// Register the project route
		try {
			CommonTx.get().data().mesh().routerStorageRegistry().addProject(project.getName());
		} catch (InvalidNameException e) {
			log.error("Failed to register project {" + project.getName() + "}");
			throw error(BAD_REQUEST, "project_error_name_already_reserved", project.getName());
		}

		// Store the project and the branch in the index
		batch.add(project.onCreated());
		batch.add(initialBranch.onCreated());

		// Add events for created basenode
		batch.add(project.getBaseNode().onCreated());
		Tx.get().contentDao().getDraftFieldContainers(project.getBaseNode()).forEach(c -> {
			batch.add(contentDao.onCreated(c, branchUuid, DRAFT));
		});

		return project;
	}

	@Override
	default boolean update(HibProject project, InternalActionContext ac, EventQueueBatch batch) {
		ProjectUpdateRequest requestModel = ac.fromJson(ProjectUpdateRequest.class);

		String oldName = project.getName();
		String newName = requestModel.getName();
		CommonTx.get().data().mesh().routerStorageRegistry().assertProjectName(newName);
		if (shouldUpdate(newName, oldName)) {
			// Check for conflicting project name
			HibProject projectWithSameName = findByName(newName);
			if (projectWithSameName != null && !projectWithSameName.getUuid().equals(project.getUuid())) {
				throw conflict(projectWithSameName.getUuid(), newName, "project_conflicting_name");
			}

			project.setName(newName);
			project.setEditor(ac.getUser());
			project.setLastEditedTimestamp();

			mergeIntoPersisted(project);

			// Update the project and its nodes in the index
			batch.add(project.onUpdated());
			return true;
		}
		return false;
	}

	@Override
	default void delete(HibProject project, BulkActionContext bac) {
		if (log.isDebugEnabled()) {
			log.debug("Deleting project {" + project.getName() + "}");
		}
		NodeDao nodeDao = CommonTx.get().nodeDao();
		SchemaDao schemaDao = CommonTx.get().schemaDao();
		MicroschemaDao microschemaDao = CommonTx.get().microschemaDao();
		TagFamilyDao tagFamilyDao = CommonTx.get().tagFamilyDao();
		BranchDao branchDao = CommonTx.get().branchDao();

		// Remove the nodes in the project hierarchy
		HibNode base = project.getBaseNode();
		nodeDao.delete(base, bac, true, true);

		// Remove the tagfamilies from the index
		tagFamilyDao.onRootDeleted(project, bac);

		// Remove all nodes in this project
		for (HibNode node : findNodes(project)) {
			nodeDao.delete(node, bac, true, false);
			bac.inc();
		}

		// Finally also remove the node root
		nodeDao.onRootDeleted(project, bac);

		// Unassign the schemas from the container
		for (HibSchema container : project.getSchemas().list()) {
			schemaDao.unassign(container, project, bac.batch());
		}

		// Unassign the microschemas from the container
		for (HibMicroschema container : project.getMicroschemas().list()) {
			microschemaDao.unassign(container, project, bac.batch());
		}

		// Remove the project schema root from the index
		schemaDao.onRootDeleted(project, bac);

		// Remove the branch root and all branches
		branchDao.onRootDeleted(project, bac);

		// Remove the project from the index
		bac.add(project.onDeleted());

		// Finally remove the project node
		deletePersisted(project);

		bac.process(true);
	}

	@Override
	default ProjectSchemaEventModel onSchemaAssignEvent(HibProject project, HibSchema schema, Assignment assigned) {
		ProjectSchemaEventModel model = new ProjectSchemaEventModel();
		switch (assigned) {
			case ASSIGNED:
				model.setEvent(PROJECT_SCHEMA_ASSIGNED);
				break;
			case UNASSIGNED:
				model.setEvent(PROJECT_SCHEMA_UNASSIGNED);
				break;
		}
		model.setProject(project.transformToReference());
		model.setSchema(schema.transformToReference());
		return model;
	}

	@Override
	default ProjectMicroschemaEventModel onMicroschemaAssignEvent(HibProject project, HibMicroschema microschema, Assignment assigned) {
		ProjectMicroschemaEventModel model = new ProjectMicroschemaEventModel();
		switch (assigned) {
			case ASSIGNED:
				model.setEvent(PROJECT_MICROSCHEMA_ASSIGNED);
				break;
			case UNASSIGNED:
				model.setEvent(PROJECT_MICROSCHEMA_UNASSIGNED);
				break;
		}
		model.setProject(project.transformToReference());
		model.setMicroschema(microschema.transformToReference());
		return model;
	}
}
