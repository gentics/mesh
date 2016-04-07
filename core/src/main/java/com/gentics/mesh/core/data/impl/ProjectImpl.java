package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_BASE_NODE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LANGUAGE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_NODE_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_RELEASE_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAGFAMILY_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAG_ROOT;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.DELETE_ACTION;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.STORE_ACTION;
import static com.gentics.mesh.core.rest.error.Errors.conflict;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.AbstractMeshCoreVertex;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.NodeRoot;
import com.gentics.mesh.core.data.root.ReleaseRoot;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.root.TagRoot;
import com.gentics.mesh.core.data.root.impl.NodeRootImpl;
import com.gentics.mesh.core.data.root.impl.ProjectSchemaContainerRootImpl;
import com.gentics.mesh.core.data.root.impl.ReleaseRootImpl;
import com.gentics.mesh.core.data.root.impl.TagFamilyRootImpl;
import com.gentics.mesh.core.data.root.impl.TagRootImpl;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.project.ProjectReference;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.project.ProjectUpdateRequest;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.graphdb.spi.Database;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Observable;

/**
 * @see Project
 */
public class ProjectImpl extends AbstractMeshCoreVertex<ProjectResponse, Project> implements Project {

	private static final Logger log = LoggerFactory.getLogger(ProjectImpl.class);

	public static void checkIndices(Database database) {
		// TODO index to name + unique constraint
		database.addVertexType(ProjectImpl.class);
	}

	@Override
	public ProjectReference createEmptyReferenceModel() {
		return new ProjectReference();
	}

	@Override
	public String getType() {
		return Project.TYPE;
	}

	@Override
	public String getName() {
		return getProperty("name");
	}

	@Override
	public void addLanguage(Language language) {
		setUniqueLinkOutTo(language.getImpl(), HAS_LANGUAGE);
	}

	@Override
	public List<? extends Language> getLanguages() {
		return out(HAS_LANGUAGE).has(LanguageImpl.class).toListExplicit(LanguageImpl.class);
	}

	@Override
	public void removeLanguage(Language language) {
		unlinkOut(language.getImpl(), HAS_LANGUAGE);
	}

	@Override
	public void setName(String name) {
		setProperty("name", name);
	}

	@Override
	public TagFamilyRoot getTagFamilyRoot() {
		TagFamilyRoot root = out(HAS_TAGFAMILY_ROOT).has(TagFamilyRootImpl.class).nextOrDefaultExplicit(TagFamilyRootImpl.class, null);
		if (root == null) {
			root = getGraph().addFramedVertex(TagFamilyRootImpl.class);
			linkOut(root.getImpl(), HAS_TAGFAMILY_ROOT);
		}
		return root;
	}

	@Override
	public SchemaContainerRoot getSchemaContainerRoot() {
		SchemaContainerRoot root = out(HAS_SCHEMA_ROOT).has(ProjectSchemaContainerRootImpl.class)
				.nextOrDefaultExplicit(ProjectSchemaContainerRootImpl.class, null);
		if (root == null) {
			root = getGraph().addFramedVertex(ProjectSchemaContainerRootImpl.class);
			linkOut(root.getImpl(), HAS_SCHEMA_ROOT);
		}
		return root;
	}

	@Override
	public Node getBaseNode() {
		return out(HAS_BASE_NODE).has(NodeImpl.class).nextOrDefaultExplicit(NodeImpl.class, null);
	}

	@Override
	public TagRoot getTagRoot() {
		TagRoot root = out(HAS_TAG_ROOT).has(TagRootImpl.class).nextOrDefaultExplicit(TagRootImpl.class, null);
		if (root == null) {
			root = getGraph().addFramedVertex(TagRootImpl.class);
			linkOut(root.getImpl(), HAS_TAG_ROOT);
		}
		return root;
	}

	@Override
	public NodeRoot getNodeRoot() {
		NodeRoot root = out(HAS_NODE_ROOT).has(NodeRootImpl.class).nextOrDefaultExplicit(NodeRootImpl.class, null);
		if (root == null) {
			root = getGraph().addFramedVertex(NodeRootImpl.class);
			linkOut(root.getImpl(), HAS_NODE_ROOT);
		}
		return root;
	}

	@Override
	public void setBaseNode(Node baseNode) {
		linkOut(baseNode.getImpl(), HAS_BASE_NODE);
	}

	@Override
	public Observable<ProjectResponse> transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		Set<Observable<ProjectResponse>> obsParts = new HashSet<>();

		ProjectResponse restProject = new ProjectResponse();
		restProject.setName(getName());
		restProject.setRootNodeUuid(getBaseNode().getUuid());

		// Add common fields
		obsParts.add(fillCommonRestFields(ac, restProject));

		// Role permissions
		obsParts.add(setRolePermissions(ac, restProject));

		// Merge and complete
		return Observable.merge(obsParts).last();
	}

	@Override
	public Node createBaseNode(User creator) {
		Node baseNode = getBaseNode();
		if (baseNode == null) {
			baseNode = getGraph().addFramedVertex(NodeImpl.class);
			baseNode.setSchemaContainer(BootstrapInitializer.getBoot().schemaContainerRoot().findByName("folder").toBlocking().single());
			baseNode.setProject(this);
			setBaseNode(baseNode);
			// Add the node to the aggregation nodes
			getNodeRoot().addNode(baseNode);
			BootstrapInitializer.getBoot().nodeRoot().addNode(baseNode);
		}
		return baseNode;
	}

	@Override
	public void delete() {
		if (log.isDebugEnabled()) {
			log.debug("Deleting project {" + getName() + "}");
		}

		RouterStorage.getIntance().removeProjectRouter(getName());
		getBaseNode().delete(true);
		getTagFamilyRoot().delete();
		getNodeRoot().delete();

		for (SchemaContainer container : getSchemaContainerRoot().findAll()) {
			getSchemaContainerRoot().removeSchemaContainer(container);
		}
		getSchemaContainerRoot().delete();
		reload();
		getVertex().remove();

	}

	@Override
	public Observable<? extends Project> update(InternalActionContext ac) {
		Database db = MeshSpringConfiguration.getInstance().database();
		ProjectUpdateRequest requestModel = ac.fromJson(ProjectUpdateRequest.class);

		return db.trx(() -> {
			if (shouldUpdate(requestModel.getName(), getName())) {
				// Check for conflicting project name
				Project projectWithSameName = MeshRoot.getInstance().getProjectRoot().findByName(requestModel.getName()).toBlocking().single();
				if (projectWithSameName != null && !projectWithSameName.getUuid().equals(getUuid())) {
					throw conflict(projectWithSameName.getUuid(), requestModel.getName(), "project_conflicting_name");
				}
				setName(requestModel.getName());
			}
			setEditor(ac.getUser());
			setLastEditedTimestamp(System.currentTimeMillis());
			return createIndexBatch(STORE_ACTION);
		}).process().map(i -> this);
	}

	@Override
	public void applyPermissions(Role role, boolean recursive, Set<GraphPermission> permissionsToGrant, Set<GraphPermission> permissionsToRevoke) {
		if (recursive) {
			getSchemaContainerRoot().applyPermissions(role, recursive, permissionsToGrant, permissionsToRevoke);
			getTagFamilyRoot().applyPermissions(role, recursive, permissionsToGrant, permissionsToRevoke);
			getNodeRoot().applyPermissions(role, recursive, permissionsToGrant, permissionsToRevoke);
		}
		super.applyPermissions(role, recursive, permissionsToGrant, permissionsToRevoke);
	}

	@Override
	public void addRelatedEntries(SearchQueueBatch batch, SearchQueueEntryAction action) {
		String baseNodeUuid = getBaseNode().getUuid();
		if (action == SearchQueueEntryAction.DELETE_ACTION) {
			for (TagFamily tagFamily : getTagFamilyRoot().findAll()) {
				batch.addEntry(tagFamily, DELETE_ACTION);
			}
			for (Node node : getNodeRoot().findAll()) {
				if (baseNodeUuid.equals(node.getUuid())) {
					continue;
				}
				batch.addEntry(node, DELETE_ACTION);
			}
			for (Tag tag : getTagRoot().findAll()) {
				batch.addEntry(tag, DELETE_ACTION);
			}
		} else {
			for (Node node : getNodeRoot().findAll()) {
				if (baseNodeUuid.equals(node.getUuid())) {
					continue;
				}
				batch.addEntry(node, STORE_ACTION);
			}
		}
	}

	@Override
	public Release getInitialRelease() {
		return getReleaseRoot().getInitialRelease();
	}

	@Override
	public Release getLatestRelease() {
		return getReleaseRoot().getLatestRelease();
	}

	@Override
	public ReleaseRoot getReleaseRoot() {
		ReleaseRoot root = out(HAS_RELEASE_ROOT).has(ReleaseRootImpl.class).nextOrDefaultExplicit(ReleaseRootImpl.class, null);
		if (root == null) {
			root = getGraph().addFramedVertex(ReleaseRootImpl.class);
			linkOut(root.getImpl(), HAS_RELEASE_ROOT);
		}
		return root;
	}
}
