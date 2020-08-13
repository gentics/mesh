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
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.ProjectDaoWrapper;
import com.gentics.mesh.core.data.generic.AbstractMeshCoreVertex;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.BranchRoot;
import com.gentics.mesh.core.data.root.MicroschemaRoot;
import com.gentics.mesh.core.data.root.NodeRoot;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.root.SchemaRoot;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.root.impl.BranchRootImpl;
import com.gentics.mesh.core.data.root.impl.NodeRootImpl;
import com.gentics.mesh.core.data.root.impl.ProjectMicroschemaContainerRootImpl;
import com.gentics.mesh.core.data.root.impl.ProjectSchemaContainerRootImpl;
import com.gentics.mesh.core.data.root.impl.TagFamilyRootImpl;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.event.MeshElementEventModel;
import com.gentics.mesh.core.rest.event.project.ProjectMicroschemaEventModel;
import com.gentics.mesh.core.rest.event.project.ProjectSchemaEventModel;
import com.gentics.mesh.core.rest.project.ProjectReference;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.event.Assignment;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.handler.VersionHandler;
import com.gentics.mesh.madl.field.FieldType;
import com.gentics.mesh.madl.traversal.TraversalResult;

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
	public SchemaRoot getSchemaContainerRoot() {
		SchemaRoot root = out(HAS_SCHEMA_ROOT, ProjectSchemaContainerRootImpl.class).nextOrNull();
		if (root == null) {
			root = getGraph().addFramedVertex(ProjectSchemaContainerRootImpl.class);
			linkOut(root, HAS_SCHEMA_ROOT);
		}
		return root;
	}

	@Override
	public MicroschemaRoot getMicroschemaContainerRoot() {
		MicroschemaRoot root = out(HAS_MICROSCHEMA_ROOT, ProjectMicroschemaContainerRootImpl.class).nextOrNull();
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

	/**
	 * @deprecated Use {@link ProjectDaoWrapper}{@link #transformToRestSync(InternalActionContext, int, String...)} instead
	 */
	@Override
	@Deprecated
	public ProjectResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		ProjectDaoWrapper projectDao= mesh().boot().projectDao();
		return projectDao.transformToRestSync(this, ac, level, languageTags);
	}

	@Override
	public Node createBaseNode(HibUser creator, SchemaVersion schemaVersion) {
		Node baseNode = getBaseNode();
		if (baseNode == null) {
			baseNode = getGraph().addFramedVertex(NodeImpl.class);
			baseNode.setSchemaContainer(schemaVersion.getSchemaContainer());
			baseNode.setProject(this);
			baseNode.setCreated(creator);
			Language language = mesh().boot().languageRoot().findByLanguageTag(mesh().boot().mesh().getOptions().getDefaultLanguage());
			baseNode.createGraphFieldContainer(language.getLanguageTag(), getLatestBranch(), creator);
			setBaseNode(baseNode);
		}
		return baseNode;
	}

	/**
	 * @deprecated Use Dao method instead.
	 */
	@Override
	@Deprecated
	public void delete(BulkActionContext bac) {
		ProjectDaoWrapper projectDao = mesh().boot().projectDao();
		projectDao.delete(this, bac);
	}

	@Override
	public boolean update(InternalActionContext ac, EventQueueBatch batch) {
		throw new RuntimeException("Wrong invocation. Use dao instead");
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
	public HibBranch getInitialBranch() {
		return getBranchRoot().getInitialBranch();
	}

	@Override
	public HibBranch getLatestBranch() {
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
		ProjectRoot projectRoot = mesh().boot().projectRoot();
		return projectRoot.getSubETag(this, ac);
	}

	@Override
	public String getAPIPath(InternalActionContext ac) {
		return VersionHandler.baseRoute(ac) + "/projects/" + getUuid();
	}

	@Override
	public HibUser getCreator() {
		return mesh().userProperties().getCreator(this);
	}

	@Override
	public HibUser getEditor() {
		return mesh().userProperties().getEditor(this);
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
	public ProjectSchemaEventModel onSchemaAssignEvent(Schema schema, Assignment assigned) {
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
	public ProjectMicroschemaEventModel onMicroschemaAssignEvent(Microschema microschema, Assignment assigned) {
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
	public HibBranch findBranch(String branchNameOrUuid) {
		return findBranchOpt(branchNameOrUuid)
			.orElseThrow(() -> error(BAD_REQUEST, "branch_error_not_found", branchNameOrUuid));
	}

	@Override
	public HibBranch findBranchOrLatest(String branchNameOrUuid) {
		return findBranchOpt(branchNameOrUuid).orElseGet(this::getLatestBranch);
	}

	private Optional<HibBranch> findBranchOpt(String branchNameOrUuid) {
		return Optional.ofNullable(mesh().branchCache().get(id() + "-" + branchNameOrUuid, key -> {
			HibBranch branch = null;

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
