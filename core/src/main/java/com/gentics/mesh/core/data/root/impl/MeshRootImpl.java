package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_GROUP_ROOT;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_LANGUAGE_ROOT;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_MICROSCHEMA_ROOT;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_NODE_ROOT;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_PROJECT_ROOT;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_ROLE_ROOT;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_SCHEMA_ROOT;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_SEARCH_QUEUE_ROOT;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_TAGFAMILY_ROOT;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_TAG_ROOT;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_USER_ROOT;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.root.GroupRoot;
import com.gentics.mesh.core.data.root.LanguageRoot;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.MicroschemaContainerRoot;
import com.gentics.mesh.core.data.root.NodeRoot;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.root.RoleRoot;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.root.SearchQueueRoot;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.root.TagRoot;
import com.gentics.mesh.core.data.root.UserRoot;

public class MeshRootImpl extends MeshVertexImpl implements MeshRoot {
	
	private static Logger log = LoggerFactory.getLogger(MeshRootImpl.class);

	private static MeshRoot instance;

	public static MeshRoot getInstance() {
		return instance;
	}

	public static void setInstance(MeshRoot meshRoot) {
		instance = meshRoot;
	}

	@Override
	public UserRoot getUserRoot() {
		UserRoot root = out(HAS_USER_ROOT).has(UserRootImpl.class).nextOrDefault(UserRootImpl.class, null);
		if (root == null) {
			root = getGraph().addFramedVertex(UserRootImpl.class);
			linkOut(root.getImpl(), HAS_USER_ROOT);
			log.info("Stored tag root {" + root.getUuid() + "}");
		}
		return root;
	}

	@Override
	public RoleRoot getRoleRoot() {
		RoleRoot root = out(HAS_ROLE_ROOT).has(RoleRootImpl.class).nextOrDefault(RoleRootImpl.class, null);
		if (root == null) {
			root = getGraph().addFramedVertex(RoleRootImpl.class);
			linkOut(root.getImpl(), HAS_ROLE_ROOT);
		}
		return root;
	}

	@Override
	public SearchQueueRoot getSearchQueueRoot() {
		SearchQueueRoot root = out(HAS_SEARCH_QUEUE_ROOT).has(SearchQueueRootImpl.class).nextOrDefault(SearchQueueRootImpl.class, null);
		if (root == null) {
			root = getGraph().addFramedVertex(SearchQueueRootImpl.class);
			linkOut(root.getImpl(), HAS_SEARCH_QUEUE_ROOT);
		}
		return root;

	}

	@Override
	public GroupRoot getGroupRoot() {
		GroupRoot root = out(HAS_GROUP_ROOT).has(GroupRootImpl.class).nextOrDefault(GroupRootImpl.class, null);
		if (root == null) {
			root = getGraph().addFramedVertex(GroupRootImpl.class);
			linkOut(root.getImpl(), HAS_GROUP_ROOT);
		}
		return root;
	}

	@Override
	public TagRoot getTagRoot() {
		TagRoot root = out(HAS_TAG_ROOT).has(TagRootImpl.class).nextOrDefault(TagRootImpl.class, null);
		if (root == null) {
			root = getGraph().addFramedVertex(TagRootImpl.class);
			linkOut(root.getImpl(), HAS_TAG_ROOT);
		}
		return root;
	}

	@Override
	public SchemaContainerRoot getSchemaContainerRoot() {
		SchemaContainerRoot root = out(HAS_SCHEMA_ROOT).has(SchemaContainerRootImpl.class).nextOrDefault(SchemaContainerRootImpl.class, null);
		if (root == null) {
			root = getGraph().addFramedVertex(SchemaContainerRootImpl.class);
			linkOut(root.getImpl(), HAS_SCHEMA_ROOT);
		}
		return root;
	}

	@Override
	public LanguageRoot getLanguageRoot() {
		LanguageRoot root = out(HAS_LANGUAGE_ROOT).has(LanguageRootImpl.class).nextOrDefault(LanguageRootImpl.class, null);
		if (root == null) {
			root = getGraph().addFramedVertex(LanguageRootImpl.class);
			linkOut(root.getImpl(), HAS_LANGUAGE_ROOT);
		}
		return root;
	}

	@Override
	public ProjectRoot getProjectRoot() {
		ProjectRoot root = out(HAS_PROJECT_ROOT).has(ProjectRootImpl.class).nextOrDefault(ProjectRootImpl.class, null);
		if (root == null) {
			root = getGraph().addFramedVertex(ProjectRootImpl.class);
			linkOut(root.getImpl(), HAS_PROJECT_ROOT);
		}
		return root;
	}

	@Override
	public NodeRoot getNodeRoot() {
		NodeRoot root = out(HAS_NODE_ROOT).has(NodeRootImpl.class).nextOrDefault(NodeRootImpl.class, null);
		if (root == null) {
			root = getGraph().addFramedVertex(NodeRootImpl.class);
			linkOut(root.getImpl(), HAS_NODE_ROOT);
		}
		return root;
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
	public MicroschemaContainerRoot getMicroschemaContainerRoot() {
		MicroschemaContainerRoot root = out(HAS_MICROSCHEMA_ROOT).has(MicroschemaContainerRootImpl.class).nextOrDefaultExplicit(
				MicroschemaContainerRootImpl.class, null);
		if (root == null) {
			root = getGraph().addFramedVertex(MicroschemaContainerRootImpl.class);
			linkOut(root.getImpl(), HAS_MICROSCHEMA_ROOT);
		}
		return root;
	}

	@Override
	public MeshRootImpl getImpl() {
		return this;
	}

}
