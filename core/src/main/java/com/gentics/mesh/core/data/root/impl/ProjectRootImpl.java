package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.PUBLISH_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PUBLISHED_PERM;
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

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.root.BranchRoot;
import com.gentics.mesh.core.data.root.MicroschemaContainerRoot;
import com.gentics.mesh.core.data.root.NodeRoot;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.rest.error.NameConflictException;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.graphdb.spi.IndexHandler;
import com.gentics.mesh.graphdb.spi.TypeHandler;
import com.gentics.mesh.router.RouterStorage;

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
		BootstrapInitializer boot = MeshInternal.get().boot();

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
		if (!creator.hasPermission(boot.projectRoot(), CREATE_PERM)) {
			throw error(FORBIDDEN, "error_missing_perm", boot.projectRoot().getUuid(), CREATE_PERM.getRestPerm().getName());
		}
		// TODO instead of this check, a constraint in the db should be added
		Project conflictingProject = boot.projectRoot().findByName(requestModel.getName());
		if (conflictingProject != null) {
			throw new NameConflictException("project_conflicting_name", projectName, conflictingProject.getUuid());
		}
		RouterStorage.assertProjectName(requestModel.getName());

		if (requestModel.getSchema() == null || !requestModel.getSchema().isSet()) {
			throw error(BAD_REQUEST, "project_error_no_schema_reference");
		}
		SchemaContainerVersion schemaContainerVersion = MeshInternal.get().boot().schemaContainerRoot().fromReference(requestModel.getSchema());

		String hostname = requestModel.getHostname();
		Boolean ssl = requestModel.getSsl();
		String pathPrefix = requestModel.getPathPrefix();
		Project project = create(projectName, hostname, ssl, pathPrefix, creator, schemaContainerVersion, uuid, batch);
		Branch initialBranch = project.getInitialBranch();
		String branchUuid = initialBranch.getUuid();

		// Add project permissions
		creator.addCRUDPermissionOnRole(this, CREATE_PERM, project);
		creator.addCRUDPermissionOnRole(this, CREATE_PERM, project.getBaseNode());
		creator.addPermissionsOnRole(this, CREATE_PERM, project.getBaseNode(), READ_PUBLISHED_PERM, PUBLISH_PERM);
		creator.addCRUDPermissionOnRole(this, CREATE_PERM, project.getTagFamilyRoot());
		creator.addCRUDPermissionOnRole(this, CREATE_PERM, project.getSchemaContainerRoot());
		creator.addCRUDPermissionOnRole(this, CREATE_PERM, project.getMicroschemaContainerRoot());
		creator.addCRUDPermissionOnRole(this, CREATE_PERM, project.getNodeRoot());
		creator.addPermissionsOnRole(this, CREATE_PERM, initialBranch);

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

}
