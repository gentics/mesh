package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_BASE_NODE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LANGUAGE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_NODE_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAGFAMILY_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAG_ROOT;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.DELETE_ACTION;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.UPDATE_ACTION;
import static com.gentics.mesh.util.VerticleHelper.processOrFail2;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;

import java.util.List;
import java.util.Set;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.AbstractIndexedVertex;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.NodeRoot;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.root.TagRoot;
import com.gentics.mesh.core.data.root.impl.NodeRootImpl;
import com.gentics.mesh.core.data.root.impl.SchemaContainerRootImpl;
import com.gentics.mesh.core.data.root.impl.TagFamilyRootImpl;
import com.gentics.mesh.core.data.root.impl.TagRootImpl;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.project.ProjectUpdateRequest;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.InternalActionContext;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class ProjectImpl extends AbstractIndexedVertex<ProjectResponse>implements Project {

	// TODO index to name + unique constraint

	private static final Logger log = LoggerFactory.getLogger(ProjectImpl.class);

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
		setLinkOutTo(language.getImpl(), HAS_LANGUAGE);
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
		SchemaContainerRoot root = out(HAS_SCHEMA_ROOT).has(SchemaContainerRootImpl.class).nextOrDefaultExplicit(SchemaContainerRootImpl.class, null);
		if (root == null) {
			root = getGraph().addFramedVertex(SchemaContainerRootImpl.class);
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
	public Project transformToRest(InternalActionContext ac, Handler<AsyncResult<ProjectResponse>> handler) {
		Database db = MeshSpringConfiguration.getInstance().database();
		db.asyncNoTrx(tc -> {
			ProjectResponse projectResponse = new ProjectResponse();
			projectResponse.setName(getName());
			projectResponse.setRootNodeUuid(getBaseNode().getUuid());
			fillRest(projectResponse, ac);
			tc.complete(projectResponse);
		} , (AsyncResult<ProjectResponse> rh) -> {
			handler.handle(rh);
		});
		return this;
	}

	@Override
	public Node createBaseNode(User creator) {
		Node baseNode = getBaseNode();
		if (baseNode == null) {
			baseNode = getGraph().addFramedVertex(NodeImpl.class);
			baseNode.setSchemaContainer(BootstrapInitializer.getBoot().schemaContainerRoot().findByName("folder"));
			baseNode.setCreator(creator);
			baseNode.setEditor(creator);
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

		RouterStorage.getRouterStorage().removeProjectRouter(getName());
		getBaseNode().delete();
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
	public void update(InternalActionContext ac, Handler<AsyncResult<Void>> handler) {
		Database db = MeshSpringConfiguration.getInstance().database();
		ProjectUpdateRequest requestModel = ac.fromJson(ProjectUpdateRequest.class);

		db.trx(txUpdate -> {
			// Check for conflicting project name
			if (requestModel.getName() != null && !getName().equals(requestModel.getName())) {
				if (MeshRoot.getInstance().getProjectRoot().findByName(requestModel.getName()) != null) {
					txUpdate.fail(new HttpStatusCodeErrorException(CONFLICT, ac.i18n("project_conflicting_name")));
					return;
				}
				setName(requestModel.getName());
			}
			setEditor(ac.getUser());
			setLastEditedTimestamp(System.currentTimeMillis());
			SearchQueueBatch batch = addIndexBatch(UPDATE_ACTION);
			txUpdate.complete(batch);
		} , (AsyncResult<SearchQueueBatch> txUpdated) -> {
			if (txUpdated.failed()) {
				handler.handle(Future.failedFuture(txUpdated.cause()));
			} else {
				processOrFail2(ac, txUpdated.result(), handler);
			}
		});
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
		if (action == SearchQueueEntryAction.DELETE_ACTION) {
			for (TagFamily tagFamily : getTagFamilyRoot().findAll()) {
				batch.addEntry(tagFamily, DELETE_ACTION);
			}
			for (Node node : getNodeRoot().findAll()) {
				batch.addEntry(node, DELETE_ACTION);
			}
			for (Tag tag : getTagRoot().findAll()) {
				batch.addEntry(tag, DELETE_ACTION);
			}
		} else {
			for (Node node : getNodeRoot().findAll()) {
				batch.addEntry(node, UPDATE_ACTION);
			}
		}
	}

}
