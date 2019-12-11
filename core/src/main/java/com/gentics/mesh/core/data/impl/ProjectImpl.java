package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_BRANCH_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LANGUAGE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_MICROSCHEMA_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_NODE_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROOT_NODE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAGFAMILY_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.PROJECT_KEY_PROPERTY;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_MICROSCHEMA_ASSIGNED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_MICROSCHEMA_UNASSIGNED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_SCHEMA_ASSIGNED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_SCHEMA_UNASSIGNED;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.madl.index.VertexIndexDefinition.vertexIndex;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Optional;
import java.util.Set;

import javax.naming.InvalidNameException;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.AbstractMeshCoreVertex;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.BranchRoot;
import com.gentics.mesh.core.data.root.MicroschemaContainerRoot;
import com.gentics.mesh.core.data.root.NodeRoot;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.root.impl.BranchRootImpl;
import com.gentics.mesh.core.data.root.impl.NodeRootImpl;
import com.gentics.mesh.core.data.root.impl.ProjectMicroschemaContainerRootImpl;
import com.gentics.mesh.core.data.root.impl.ProjectSchemaContainerRootImpl;
import com.gentics.mesh.core.data.root.impl.TagFamilyRootImpl;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.rest.event.MeshElementEventModel;
import com.gentics.mesh.core.rest.event.project.ProjectMicroschemaEventModel;
import com.gentics.mesh.core.rest.event.project.ProjectSchemaEventModel;
import com.gentics.mesh.core.rest.project.ProjectReference;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.project.ProjectUpdateRequest;
import com.gentics.mesh.event.Assignment;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.handler.VersionHandler;
import com.gentics.mesh.madl.field.FieldType;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.value.FieldsSet;

import io.reactivex.Single;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @see Project
 */
public class ProjectImpl extends AbstractMeshCoreVertex<ProjectResponse, Project> implements Project {

	private static final Logger log = LoggerFactory.getLogger(ProjectImpl.class);

	public static void init(TypeHandler type, IndexHandler index) {
		// TODO index to name + unique constraint
		type.createVertexType(ProjectImpl.class, MeshVertexImpl.class);
		index.createIndex(vertexIndex(ProjectImpl.class)
			.withField("name", FieldType.STRING)
			.unique());
	}

	@Override
	public ProjectReference transformToReference() {
		return new ProjectReference().setName(getName()).setUuid(getUuid());
	}

	@Override
	public String getName() {
		return property("name");
	}

	@Override
	public void addLanguage(Language language) {
		setUniqueLinkOutTo(language, HAS_LANGUAGE);
	}

	@Override
	public TraversalResult<? extends Language> getLanguages() {
		return out(HAS_LANGUAGE, LanguageImpl.class);
	}

	@Override
	public void removeLanguage(Language language) {
		unlinkOut(language, HAS_LANGUAGE);
	}

	@Override
	public void setName(String name) {
		property("name", name);
	}

	@Override
	public TagFamilyRoot getTagFamilyRoot() {
		TagFamilyRoot root = out(HAS_TAGFAMILY_ROOT, TagFamilyRootImpl.class).nextOrNull();
		if (root == null) {
			root = getGraph().addFramedVertex(TagFamilyRootImpl.class);
			linkOut(root, HAS_TAGFAMILY_ROOT);
		}
		return root;
	}

	@Override
	public SchemaContainerRoot getSchemaContainerRoot() {
		SchemaContainerRoot root = out(HAS_SCHEMA_ROOT, ProjectSchemaContainerRootImpl.class).nextOrNull();
		if (root == null) {
			root = getGraph().addFramedVertex(ProjectSchemaContainerRootImpl.class);
			linkOut(root, HAS_SCHEMA_ROOT);
		}
		return root;
	}

	@Override
	public MicroschemaContainerRoot getMicroschemaContainerRoot() {
		MicroschemaContainerRoot root = out(HAS_MICROSCHEMA_ROOT, ProjectMicroschemaContainerRootImpl.class).nextOrNull();
		if (root == null) {
			root = getGraph().addFramedVertex(ProjectMicroschemaContainerRootImpl.class);
			linkOut(root, HAS_MICROSCHEMA_ROOT);
		}
		return root;
	}

	@Override
	public Node getBaseNode() {
		return out(HAS_ROOT_NODE, NodeImpl.class).nextOrNull();
	}

	@Override
	public NodeRoot getNodeRoot() {
		NodeRoot root = out(HAS_NODE_ROOT, NodeRootImpl.class).nextOrNull();
		if (root == null) {
			root = getGraph().addFramedVertex(NodeRootImpl.class);
			linkOut(root, HAS_NODE_ROOT);
		}
		return root;
	}

	@Override
	public void setBaseNode(Node baseNode) {
		linkOut(baseNode, HAS_ROOT_NODE);
	}

	@Override
	public ProjectResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		GenericParameters generic = ac.getGenericParameters();
		FieldsSet fields = generic.getFields();

		ProjectResponse restProject = new ProjectResponse();
		if (fields.has("name")) {
			restProject.setName(getName());
		}
		if (fields.has("rootNode")) {
			restProject.setRootNode(getBaseNode().transformToReference(ac));
		}

		fillCommonRestFields(ac, fields, restProject);
		setRolePermissions(ac, restProject);

		return restProject;
	}

	@Override
	public Node createBaseNode(User creator, SchemaContainerVersion schemaContainerVersion) {
		Node baseNode = getBaseNode();
		if (baseNode == null) {
			baseNode = getGraph().addFramedVertex(NodeImpl.class);
			baseNode.setSchemaContainer(schemaContainerVersion.getSchemaContainer());
			baseNode.setProject(this);
			baseNode.setCreated(creator);
			Language language = mesh().boot().languageRoot().findByLanguageTag(mesh().boot().mesh().getOptions().getDefaultLanguage());
			baseNode.createGraphFieldContainer(language.getLanguageTag(), getLatestBranch(), creator);
			setBaseNode(baseNode);
		}
		return baseNode;
	}

	@Override
	public void delete(BulkActionContext bac) {
		if (log.isDebugEnabled()) {
			log.debug("Deleting project {" + getName() + "}");
		}

		// Remove the nodes in the project hierarchy
		Node base = getBaseNode();
		base.delete(bac, true, true);

		// Remove the tagfamilies from the index
		getTagFamilyRoot().delete(bac);

		// Remove all nodes in this project
		for (Node node : findNodes()) {
			node.delete(bac, true, false);
			bac.inc();
		}

		// Finally also remove the node root
		getNodeRoot().delete(bac);

		// Unassign the schema from the container
		for (SchemaContainer container : getSchemaContainerRoot().findAll()) {
			getSchemaContainerRoot().removeSchemaContainer(container, bac.batch());
		}

		// Remove the project schema root from the index
		getSchemaContainerRoot().delete(bac);

		// Remove the branch root and all branches
		getBranchRoot().delete(bac);

		// Remove the project from the index
		bac.add(onDeleted());

		// Finally remove the project node
		getVertex().remove();

		bac.process(true);
	}

	@Override
	public boolean update(InternalActionContext ac, EventQueueBatch batch) {
		ProjectUpdateRequest requestModel = ac.fromJson(ProjectUpdateRequest.class);

		String oldName = getName();
		String newName = requestModel.getName();
		mesh().routerStorageRegistry().assertProjectName(newName);
		if (shouldUpdate(newName, oldName)) {
			// Check for conflicting project name
			Project projectWithSameName = mesh().boot().meshRoot().getProjectRoot().findByName(newName);
			if (projectWithSameName != null && !projectWithSameName.getUuid().equals(getUuid())) {
				throw conflict(projectWithSameName.getUuid(), newName, "project_conflicting_name");
			}

			setName(newName);
			setEditor(ac.getUser());
			setLastEditedTimestamp();

			// Update the project and its nodes in the index
			batch.add(onUpdated());
			return true;
		}
		return false;
	}

	@Override
	public void applyPermissions(EventQueueBatch batch, Role role, boolean recursive, Set<GraphPermission> permissionsToGrant,
		Set<GraphPermission> permissionsToRevoke) {
		if (recursive) {
			getTagFamilyRoot().applyPermissions(batch, role, recursive, permissionsToGrant, permissionsToRevoke);
			getBranchRoot().applyPermissions(batch, role, recursive, permissionsToGrant, permissionsToRevoke);
			getBaseNode().applyPermissions(batch, role, recursive, permissionsToGrant, permissionsToRevoke);
		}
		super.applyPermissions(batch, role, recursive, permissionsToGrant, permissionsToRevoke);
	}

	@Override
	public Branch getInitialBranch() {
		return getBranchRoot().getInitialBranch();
	}

	@Override
	public Branch getLatestBranch() {
		return getBranchRoot().getLatestBranch();
	}

	@Override
	public BranchRoot getBranchRoot() {
		BranchRoot root = out(HAS_BRANCH_ROOT, BranchRootImpl.class).nextOrNull();
		if (root == null) {
			root = getGraph().addFramedVertex(BranchRootImpl.class);
			linkOut(root, HAS_BRANCH_ROOT);
		}
		return root;
	}

	@Override
	public String getSubETag(InternalActionContext ac) {
		return String.valueOf(getLastEditedTimestamp());
	}

	@Override
	public String getAPIPath(InternalActionContext ac) {
		return VersionHandler.baseRoute(ac) + "/projects/" + getUuid();
	}

	@Override
	public User getCreator() {
		return mesh().userProperties().getCreator(this);
	}

	@Override
	public User getEditor() {
		return mesh().userProperties().getEditor(this);
	}

	@Override
	public Single<ProjectResponse> transformToRest(InternalActionContext ac, int level, String... languageTags) {
		return db().asyncTx(() -> {
			return Single.just(transformToRestSync(ac, level, languageTags));
		});
	}

	@Override
	public MeshElementEventModel onCreated() {
		MeshElementEventModel event = super.onCreated();
		try {
			mesh().routerStorageRegistry().addProject(getName());
		} catch (InvalidNameException e) {
			log.error("Failed to register project {" + getName() + "}");
			throw error(BAD_REQUEST, "project_error_name_already_reserved", getName());
		}
		return event;
	}

	@Override
	public ProjectSchemaEventModel onSchemaAssignEvent(SchemaContainer schema, Assignment assigned) {
		ProjectSchemaEventModel model = new ProjectSchemaEventModel();
		switch (assigned) {
		case ASSIGNED:
			model.setEvent(PROJECT_SCHEMA_ASSIGNED);
			break;
		case UNASSIGNED:
			model.setEvent(PROJECT_SCHEMA_UNASSIGNED);
			break;
		}
		model.setProject(transformToReference());
		model.setSchema(schema.transformToReference());
		return model;
	}

	@Override
	public ProjectMicroschemaEventModel onMicroschemaAssignEvent(MicroschemaContainer microschema, Assignment assigned) {
		ProjectMicroschemaEventModel model = new ProjectMicroschemaEventModel();
		switch (assigned) {
		case ASSIGNED:
			model.setEvent(PROJECT_MICROSCHEMA_ASSIGNED);
			break;
		case UNASSIGNED:
			model.setEvent(PROJECT_MICROSCHEMA_UNASSIGNED);
			break;
		}
		model.setProject(transformToReference());
		model.setMicroschema(microschema.transformToReference());
		return model;
	}

	@Override
	public TraversalResult<? extends Node> findNodes() {
		return db().getVerticesTraversal(NodeImpl.class, new String[] { PROJECT_KEY_PROPERTY }, new Object[] { getUuid() });
	}

	@Override
	public Node findNode(String uuid) {
		return db().getVerticesTraversal(NodeImpl.class,
			new String[] { PROJECT_KEY_PROPERTY, "uuid" },
			new Object[] { getUuid(), uuid }).nextOrNull();
	}

	@Override
	public Branch findBranch(String branchNameOrUuid) {
		return findBranchOpt(branchNameOrUuid)
			.orElseThrow(() -> error(BAD_REQUEST, "branch_error_not_found", branchNameOrUuid));
	}

	@Override
	public Branch findBranchOrLatest(String branchNameOrUuid) {
		return findBranchOpt(branchNameOrUuid).orElseGet(this::getLatestBranch);
	}

	private Optional<Branch> findBranchOpt(String branchNameOrUuid) {
		return Optional.ofNullable(mesh().branchCache().get(id() + "-" + branchNameOrUuid, key -> {
			Branch branch = null;

			if (!isEmpty(branchNameOrUuid)) {
				branch = getBranchRoot().findByUuid(branchNameOrUuid);
				if (branch == null) {
					branch = getBranchRoot().findByName(branchNameOrUuid);
				}
				if (branch == null) {
					return null;
				}
			} else {
				branch = getLatestBranch();
			}

			return branch;
		}));
	}
}
