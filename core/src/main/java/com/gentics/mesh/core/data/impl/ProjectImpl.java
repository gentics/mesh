package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.ContainerType.DRAFT;
import static com.gentics.mesh.core.data.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_BRANCH_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_CREATOR;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_EDITOR;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LANGUAGE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_MICROSCHEMA_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_NODE_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROOT_NODE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAGFAMILY_ROOT;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.naming.InvalidNameException;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.HandleElementAction;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
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
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.context.impl.GenericEntryContextImpl;
import com.gentics.mesh.core.rest.project.ProjectReference;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.project.ProjectUpdateRequest;
import com.gentics.mesh.dagger.DB;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.LegacyDatabase;
import com.gentics.mesh.graphdb.spi.FieldType;
import com.gentics.mesh.madlmigration.TraversalResult;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.value.FieldsSet;
import com.gentics.mesh.router.RouterStorage;
import com.gentics.mesh.util.ETag;

import io.reactivex.Single;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @see Project
 */
public class ProjectImpl extends AbstractMeshCoreVertex<ProjectResponse, Project> implements Project {

	private static final Logger log = LoggerFactory.getLogger(ProjectImpl.class);

	public static void init(LegacyDatabase database) {
		// TODO index to name + unique constraint
		database.addVertexType(ProjectImpl.class, MeshVertexImpl.class);
		database.addVertexIndex(ProjectImpl.class, true, "name", FieldType.STRING);
	}

	@Override
	public ProjectReference transformToReference() {
		return new ProjectReference().setName(getName()).setUuid(getUuid());
	}

	@Override
	public String getName() {
		return value("name");
	}

	@Override
	public void addLanguage(Language language) {
		setUniqueLinkOutTo(language, HAS_LANGUAGE);
	}

	@Override
	public TraversalResult<? extends Language> getLanguages() {
		return new TraversalResult<>(out(HAS_LANGUAGE).frameExplicit((LanguageImpl.class)));
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
		TagFamilyRoot root = out(HAS_TAGFAMILY_ROOT).nextOrDefaultExplicit(TagFamilyRootImpl.class, null);
		if (root == null) {
			root = createVertex(TagFamilyRootImpl.class);
			addEdge(root, HAS_TAGFAMILY_ROOT);
		}
		return root;
	}

	@Override
	public SchemaContainerRoot getSchemaContainerRoot() {
		SchemaContainerRoot root = out(HAS_SCHEMA_ROOT).nextOrDefaultExplicit(ProjectSchemaContainerRootImpl.class, null);
		if (root == null) {
			root = createVertex(ProjectSchemaContainerRootImpl.class);
			addEdgeOut(root, HAS_SCHEMA_ROOT);
		}
		return root;
	}

	@Override
	public MicroschemaContainerRoot getMicroschemaContainerRoot() {
		MicroschemaContainerRoot root = out(HAS_MICROSCHEMA_ROOT).frameExplicit(ProjectMicroschemaContainerRootImpl.class).firstOrNull();
		if (root == null) {
			root = getTx().createVertex(ProjectMicroschemaContainerRootImpl.class);
			addEdgeOut(root, HAS_MICROSCHEMA_ROOT);
		}
		return root;
	}

	@Override
	public Node getBaseNode() {
		return out(HAS_ROOT_NODE).frameExplicit(NodeImpl.class).firstOrNull();
	}

	@Override
	public NodeRoot getNodeRoot() {
		NodeRoot root = out(HAS_NODE_ROOT).frameExplicit(NodeRootImpl.class).firstOrNull();
		if (root == null) {
			root = getTx().createVertex(NodeRootImpl.class);
			addEdge(root, HAS_NODE_ROOT);
		}
		return root;
	}

	@Override
	public void setBaseNode(Node baseNode) {
		addEdge(baseNode, HAS_ROOT_NODE);
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
			baseNode = getTx().createVertex(NodeImpl.class);
			baseNode.setSchemaContainer(schemaContainerVersion.getSchemaContainer());
			baseNode.setProject(this);
			baseNode.setCreated(creator);
			Language language = MeshInternal.get().boot().languageRoot().findByLanguageTag(Mesh.mesh().getOptions().getDefaultLanguage());
			baseNode.createGraphFieldContainer(language, getLatestBranch(), creator);
			setBaseNode(baseNode);
			// Add the node to the aggregation nodes
			getNodeRoot().addNode(baseNode);
			MeshInternal.get().boot().nodeRoot().addNode(baseNode);
		}
		return baseNode;
	}

	@Override
	public void delete(BulkActionContext bac) {
		if (log.isDebugEnabled()) {
			log.debug("Deleting project {" + getName() + "}");
		}


		Set<String> indices = new HashSet<>();
		// Drop all node indices for all releases and all schema versions
		for (Branch branch : getBranchRoot().findAll()) {
			for (SchemaContainerVersion version : branch.findActiveSchemaVersions()) {
				for (ContainerType type : Arrays.asList(DRAFT, PUBLISHED)) {
					String index = NodeGraphFieldContainer.composeIndexName(getUuid(), branch.getUuid(), version.getUuid(), type);
					if (log.isDebugEnabled()) {
						log.debug("Adding drop entry for index {" + index + "}");
					}
					indices.add(index);
				}
			}
		}

		// Remove the nodes in the project hierarchy
		Node base = getBaseNode();
		base.deleteFully(bac, true);

		// Remove the tagfamilies from the index
		getTagFamilyRoot().delete(bac);

		// Finally also remove the node root
		getNodeRoot().delete(bac);

		// Unassign the schema from the container
		for (SchemaContainer container : getSchemaContainerRoot().findAll()) {
			getSchemaContainerRoot().removeSchemaContainer(container);
		}

		// Remove the project schema root from the index
		getSchemaContainerRoot().delete(bac);

		// Remove the branch root and all branches
		getBranchRoot().delete(bac);

		// Finally remove the project node
		getVertex().remove();

		// Remove the project from the index
		bac.batch().delete(this, false);

		bac.process(true);

		for (String index : indices) {
			bac.dropIndex(index);
		}

		// Drop the project specific indices
		bac.dropIndex(TagFamily.composeIndexName(getUuid()));
		bac.dropIndex(Tag.composeIndexName(getUuid()));
		bac.batch().processSync();
	}

	@Override
	public boolean update(InternalActionContext ac, SearchQueueBatch batch) {
		ProjectUpdateRequest requestModel = ac.fromJson(ProjectUpdateRequest.class);

		String oldName = getName();
		String newName = requestModel.getName();
		RouterStorage.assertProjectName(newName);
		if (shouldUpdate(newName, oldName)) {
			// Check for conflicting project name
			Project projectWithSameName = MeshInternal.get().boot().meshRoot().getProjectRoot().findByName(newName);
			if (projectWithSameName != null && !projectWithSameName.getUuid().equals(getUuid())) {
				throw conflict(projectWithSameName.getUuid(), newName, "project_conflicting_name");
			}

			setName(newName);
			setEditor(ac.getUser());
			setLastEditedTimestamp();

			// Update the project and its nodes in the index
			batch.store(this, true);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void applyPermissions(SearchQueueBatch batch, Role role, boolean recursive, Set<GraphPermission> permissionsToGrant,
		Set<GraphPermission> permissionsToRevoke) {
		if (recursive) {
			getSchemaContainerRoot().applyPermissions(batch, role, recursive, permissionsToGrant, permissionsToRevoke);
			getMicroschemaContainerRoot().applyPermissions(batch, role, recursive, permissionsToGrant, permissionsToRevoke);
			getTagFamilyRoot().applyPermissions(batch, role, recursive, permissionsToGrant, permissionsToRevoke);
			getNodeRoot().applyPermissions(batch, role, recursive, permissionsToGrant, permissionsToRevoke);
		}
		super.applyPermissions(batch, role, recursive, permissionsToGrant, permissionsToRevoke);
	}

	@Override
	public void handleRelatedEntries(HandleElementAction action) {
		// Check whether a base node exits. The base node may have been deleted.
		// In that case we can't handle related entries
		if (getBaseNode() == null) {
			return;
		}
		// All nodes of all branches are related to this project. All
		// nodes/containers must be updated if the project name changes.
		for (Node node : getNodeRoot().findAll()) {
			action.call(node, new GenericEntryContextImpl());
		}

		for (TagFamily family : getTagFamilyRoot().findAll()) {
			for (Tag tag : family.findAll()) {
				action.call(tag, new GenericEntryContextImpl().setProjectUuid(getUuid()));
			}
		}

		for (TagFamily tagFamily : getTagFamilyRoot().findAll()) {
			action.call(tagFamily, new GenericEntryContextImpl().setProjectUuid(getUuid()));
		}
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
		BranchRoot root = out(HAS_BRANCH_ROOT).frameExplicit(BranchRootImpl.class).firstOrNull();
		if (root == null) {
			root = getTx().createVertex(BranchRootImpl.class);
			addEdgeOut(root, HAS_BRANCH_ROOT);
		}
		return root;
	}

	@Override
	public String getETag(InternalActionContext ac) {
		return ETag.hash(getUuid() + "-" + getLastEditedTimestamp());
	}

	@Override
	public String getAPIPath(InternalActionContext ac) {
		return "/api/v1/projects/" + getUuid();
	}

	@Override
	public User getCreator() {
		return out(HAS_CREATOR).frameExplicit(UserImpl.class).firstOrNull();
	}

	@Override
	public User getEditor() {
		return out(HAS_EDITOR).nextOrDefaultExplicit(UserImpl.class, null);
	}

	@Override
	public Single<ProjectResponse> transformToRest(InternalActionContext ac, int level, String... languageTags) {
		return DB.get().asyncTx(() -> {
			return Single.just(transformToRestSync(ac, level, languageTags));
		});
	}

	@Override
	public void onCreated() {
		super.onCreated();
		try {
			RouterStorage.addProject(getName());
		} catch (InvalidNameException e) {
			log.error("Failed to register project {" + getName() + "}");
			throw error(BAD_REQUEST, "project_error_name_already_reserved", getName());
		}
	}

}
