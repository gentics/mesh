package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_PROJECT;
import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.madl.index.EdgeIndexDefinition.edgeIndex;
import static com.gentics.mesh.madl.type.EdgeTypeDefinition.edgeType;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import java.util.Stack;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.dao.ProjectDaoWrapper;
import com.gentics.mesh.core.data.dao.SchemaDaoWrapper;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.root.BranchRoot;
import com.gentics.mesh.core.data.root.MicroschemaContainerRoot;
import com.gentics.mesh.core.data.root.NodeRoot;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.error.NameConflictException;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.value.FieldsSet;

/**
 * @see ProjectRoot
 */
public class ProjectRootImpl extends AbstractRootVertex<Project> implements ProjectRoot {

	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(ProjectRootImpl.class, MeshVertexImpl.class);
		type.createType(edgeType(HAS_PROJECT));
		index.createIndex(edgeIndex(HAS_PROJECT).withInOut().withOut());
	}

	@Override
	public Class<? extends Project> getPersistanceClass() {
		return ProjectImpl.class;
	}

	@Override
	public String getRootLabel() {
		return HAS_PROJECT;
	}

	@Override
	public void addProject(Project project) {
		addItem(project);
	}

	@Override
	public void removeProject(Project project) {
		removeItem(project);
	}

	@Override
	public Project findByName(String name) {
		return mesh().projectNameCache().get(name, n -> {
			return super.findByName(n);
		});
	}

	@Override
	public Project create(String name, String hostname, Boolean ssl, String pathPrefix, User creator, SchemaContainerVersion schemaContainerVersion,
		String uuid, EventQueueBatch batch) {
		Project project = getGraph().addFramedVertex(ProjectImpl.class);
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
		branch.assignSchemaVersion(creator, schemaContainerVersion, batch);

		// Assign the provided schema container to the project
		project.getSchemaContainerRoot().addItem(schemaContainerVersion.getSchemaContainer());
		// project.getLatestBranch().assignSchemaVersion(creator,
		// schemaContainerVersion);
		project.createBaseNode(creator, schemaContainerVersion);

		project.setCreated(creator);
		project.setEditor(creator);
		project.getSchemaContainerRoot();
		project.getTagFamilyRoot();

		addItem(project);

		return project;
	}

	@Override
	public MeshVertex resolveToElement(Stack<String> stack) {
		if (stack.isEmpty()) {
			return this;
		} else {
			String uuidOrNameSegment = stack.pop();

			// Try to locate the project by name first.
			Project project = findByUuid(uuidOrNameSegment);
			if (project == null) {
				// Fallback to locate the project by name instead
				project = findByName(uuidOrNameSegment);
			}
			if (project == null) {
				return null;
			}

			if (stack.isEmpty()) {
				return project;
			} else {
				String nestedRootNode = stack.pop();
				switch (nestedRootNode) {
				case BranchRoot.TYPE:
					BranchRoot branchRoot = project.getBranchRoot();
					return branchRoot.resolveToElement(stack);
				case TagFamilyRoot.TYPE:
					TagFamilyRoot tagFamilyRoot = project.getTagFamilyRoot();
					return tagFamilyRoot.resolveToElement(stack);
				case SchemaContainerRoot.TYPE:
					SchemaContainerRoot schemaRoot = project.getSchemaContainerRoot();
					return schemaRoot.resolveToElement(stack);
				case MicroschemaContainerRoot.TYPE:
					MicroschemaContainerRoot microschemaRoot = project.getMicroschemaContainerRoot();
					return microschemaRoot.resolveToElement(stack);
				case NodeRoot.TYPE:
					NodeRoot nodeRoot = project.getNodeRoot();
					return nodeRoot.resolveToElement(stack);
				default:
					throw error(NOT_FOUND, "Unknown project element {" + nestedRootNode + "}");
				}
			}
		}

	}

	@Override
	public void delete(BulkActionContext bac) {
		throw new NotImplementedException("The project root should never be deleted.");
	}

	@Override
	public Project create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		BootstrapInitializer boot = mesh().boot();
		UserDaoWrapper userDao = boot.userDao();
		ProjectDaoWrapper projectDao = Tx.get().data().projectDao();
		SchemaDaoWrapper schemaDao = mesh().boot().schemaDao();

		// TODO also create a default object schema for the project. Move this
		// into service class
		// ObjectSchema defaultContentSchema = objectSchemaRoot.findByName(,
		// name)
		ProjectCreateRequest requestModel = ac.fromJson(ProjectCreateRequest.class);
		String projectName = requestModel.getName();
		MeshAuthUser creator = ac.getUser();

		if (StringUtils.isEmpty(requestModel.getName())) {
			throw error(BAD_REQUEST, "project_missing_name");
		}
		if (!userDao.hasPermission(creator, projectDao, CREATE_PERM)) {
			throw error(FORBIDDEN, "error_missing_perm", projectDao.getUuid(), CREATE_PERM.getRestPerm().getName());
		}
		// TODO instead of this check, a constraint in the db should be added
		Project conflictingProject = projectDao.findByName(requestModel.getName());
		if (conflictingProject != null) {
			throw new NameConflictException("project_conflicting_name", projectName, conflictingProject.getUuid());
		}
		mesh().routerStorageRegistry().assertProjectName(requestModel.getName());

		if (requestModel.getSchema() == null || !requestModel.getSchema().isSet()) {
			throw error(BAD_REQUEST, "project_error_no_schema_reference");
		}
		SchemaContainerVersion schemaContainerVersion = schemaDao.fromReference(requestModel.getSchema());

		String hostname = requestModel.getHostname();
		Boolean ssl = requestModel.getSsl();
		String pathPrefix = requestModel.getPathPrefix();
		Project project = create(projectName, hostname, ssl, pathPrefix, creator, schemaContainerVersion, uuid, batch);
		Branch initialBranch = project.getInitialBranch();
		String branchUuid = initialBranch.getUuid();

		// Add project permissions
		userDao.addCRUDPermissionOnRole(creator, this, CREATE_PERM, project);
		userDao.inheritRolePermissions(creator, project, project.getBaseNode());
		userDao.inheritRolePermissions(creator, project, project.getTagFamilyRoot());
		userDao.inheritRolePermissions(creator, project, project.getSchemaContainerRoot());
		userDao.inheritRolePermissions(creator, project, project.getMicroschemaContainerRoot());
		userDao.inheritRolePermissions(creator, project, project.getNodeRoot());
		userDao.inheritRolePermissions(creator, project, initialBranch);

		// Store the project and the branch in the index
		batch.add(project.onCreated());
		batch.add(initialBranch.onCreated());

		// Add events for created basenode
		batch.add(project.getBaseNode().onCreated());
		project.getBaseNode().getDraftGraphFieldContainers().forEach(c -> {
			batch.add(c.onCreated(branchUuid, DRAFT));
		});

		return project;

	}

	@Override
	public ProjectResponse transformToRestSync(Project project, InternalActionContext ac, int level, String... languageTags) {

		GenericParameters generic = ac.getGenericParameters();
		FieldsSet fields = generic.getFields();

		ProjectResponse restProject = new ProjectResponse();
		if (fields.has("name")) {
			restProject.setName(project.getName());
		}
		if (fields.has("rootNode")) {
			restProject.setRootNode(project.getBaseNode().transformToReference(ac));
		}

		project.fillCommonRestFields(ac, fields, restProject);
		setRolePermissions(project, ac, restProject);

		return restProject;

	}

	@Override
	public void delete(Project project, BulkActionContext bac) {
		if (log.isDebugEnabled()) {
			log.debug("Deleting project {" + project.getName() + "}");
		}

		// Remove the nodes in the project hierarchy
		Node base = project.getBaseNode();
		base.delete(bac, true, true);

		// Remove the tagfamilies from the index
		project.getTagFamilyRoot().delete(bac);

		// Remove all nodes in this project
		for (Node node : project.findNodes()) {
			node.delete(bac, true, false);
			bac.inc();
		}

		// Finally also remove the node root
		project.getNodeRoot().delete(bac);

		// Unassign the schema from the container
		for (SchemaContainer container : project.getSchemaContainerRoot().findAll()) {
			project.getSchemaContainerRoot().removeSchemaContainer(container, bac.batch());
		}

		// Remove the project schema root from the index
		project.getSchemaContainerRoot().delete(bac);

		// Remove the branch root and all branches
		project.getBranchRoot().delete(bac);

		// Remove the project from the index
		bac.add(project.onDeleted());

		// Finally remove the project node
		project.getVertex().remove();

		bac.process(true);

	}

	@Override
	public String getSubETag(Project project, InternalActionContext ac) {
		return String.valueOf(project.getLastEditedTimestamp());
	}

}
