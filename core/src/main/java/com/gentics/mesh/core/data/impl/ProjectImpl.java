package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_BASE_NODE;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_LANGUAGE;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_NODE_ROOT;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_SCHEMA_ROOT;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_TAGFAMILY_ROOT;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_TAG_ROOT;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

import java.util.List;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.AbstractGenericVertex;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.root.NodeRoot;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.root.TagRoot;
import com.gentics.mesh.core.data.root.impl.NodeRootImpl;
import com.gentics.mesh.core.data.root.impl.SchemaContainerRootImpl;
import com.gentics.mesh.core.data.root.impl.TagFamilyRootImpl;
import com.gentics.mesh.core.data.root.impl.TagRootImpl;
import com.gentics.mesh.core.rest.project.ProjectResponse;

public class ProjectImpl extends AbstractGenericVertex<ProjectResponse> implements Project {

	// TODO index to name + unique constraint

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
	public Project transformToRest(RoutingContext rc, Handler<AsyncResult<ProjectResponse>> handler) {
		ProjectResponse projectResponse = new ProjectResponse();
		projectResponse.setName(getName());
		projectResponse.setRootNodeUuid(getBaseNode().getUuid());
		fillRest(projectResponse, rc);
		handler.handle(Future.succeededFuture(projectResponse));
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
		// TODO handle this correctly
		getVertex().remove();
		// TODO handle: routerStorage.removeProjectRouter(name);
	}

}
