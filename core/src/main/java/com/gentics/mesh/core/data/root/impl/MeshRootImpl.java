package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_GROUP_ROOT;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_LANGUAGE_ROOT;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_NODE_ROOT;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_PROJECT_ROOT;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_ROLE_ROOT;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_SCHEMA_ROOT;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_TAGFAMILY_ROOT;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_TAG_ROOT;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_USER_ROOT;

import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.root.GroupRoot;
import com.gentics.mesh.core.data.root.LanguageRoot;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.NodeRoot;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.root.RoleRoot;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.root.TagRoot;
import com.gentics.mesh.core.data.root.UserRoot;

public class MeshRootImpl extends MeshVertexImpl implements MeshRoot {

	private static MeshRoot instance;

	public static MeshRoot getInstance() {
		return instance;
	}

	public static void setInstance(MeshRoot meshRoot) {
		instance = meshRoot;
	}

	@Override
	public UserRoot getUserRoot() {
		return out(HAS_USER_ROOT).has(UserRootImpl.class).nextOrDefault(UserRootImpl.class, null);
	}

	@Override
	public void setUserRoot(UserRoot userRoot) {
		linkOut(userRoot.getImpl(), HAS_USER_ROOT);
	}

	@Override
	public RoleRoot getRoleRoot() {
		return out(HAS_ROLE_ROOT).has(RoleRootImpl.class).nextOrDefault(RoleRootImpl.class, null);
	}

	@Override
	public void setRoleRoot(RoleRoot roleRoot) {
		linkOut(roleRoot.getImpl(), HAS_ROLE_ROOT);
	}

	@Override
	public GroupRoot getGroupRoot() {
		return out(HAS_GROUP_ROOT).has(GroupRootImpl.class).nextOrDefault(GroupRootImpl.class, null);
	}

	@Override
	public void setGroupRoot(GroupRoot groupRoot) {
		linkOut(groupRoot.getImpl(), HAS_GROUP_ROOT);
	}

	@Override
	public TagRoot getTagRoot() {
		return out(HAS_TAG_ROOT).has(TagRootImpl.class).nextOrDefault(TagRootImpl.class, null);
	}

	@Override
	public SchemaContainerRoot getSchemaContainerRoot() {
		return out(HAS_SCHEMA_ROOT).has(SchemaContainerRootImpl.class).nextOrDefault(SchemaContainerRootImpl.class, null);
	}

	@Override
	public void setSchemaRoot(SchemaContainerRoot schemaRoot) {
		linkOut(schemaRoot.getImpl(), HAS_SCHEMA_ROOT);
	}

	@Override
	public LanguageRoot getLanguageRoot() {
		return out(HAS_LANGUAGE_ROOT).has(LanguageRootImpl.class).nextOrDefault(LanguageRootImpl.class, null);
	}

	@Override
	public void setLanguageRoot(LanguageRoot languageRoot) {
		linkOut(languageRoot.getImpl(), HAS_LANGUAGE_ROOT);
	}

	@Override
	public ProjectRoot getProjectRoot() {
		return out(HAS_PROJECT_ROOT).has(ProjectRootImpl.class).nextOrDefault(ProjectRootImpl.class, null);
	}

	@Override
	public MeshRootImpl getImpl() {
		return this;
	}

	@Override
	public NodeRoot getNodeRoot() {
		return out(HAS_NODE_ROOT).has(NodeRootImpl.class).nextOrDefault(NodeRootImpl.class, null);
	}

	@Override
	public TagRoot createTagRoot() {
		TagRootImpl tagRoot = getGraph().addFramedVertex(TagRootImpl.class);
		setTagRoot(tagRoot);
		return tagRoot;
	}

	@Override
	public TagFamilyRoot createTagFamilyRoot() {
		TagFamilyRoot tagFamilyRoot = getGraph().addFramedVertex(TagFamilyRootImpl.class);
		setTagFamilyRoot(tagFamilyRoot);
		return tagFamilyRoot;
	}

	@Override
	public void setTagRoot(TagRoot tagRoot) {
		linkOut(tagRoot.getImpl(), HAS_TAG_ROOT);
	}

	@Override
	public NodeRoot createNodeRoot() {
		NodeRootImpl nodeRoot = getGraph().addFramedVertex(NodeRootImpl.class);
		setNodeRoot(nodeRoot);
		return nodeRoot;
	}

	@Override
	public void setNodeRoot(NodeRoot nodeRoot) {
		linkOut(nodeRoot.getImpl(), HAS_NODE_ROOT);
	}

	@Override
	public void setTagFamilyRoot(TagFamilyRoot root) {
		linkOut(root.getImpl(), HAS_TAGFAMILY_ROOT);
	}

	@Override
	public TagFamilyRoot getTagFamilyRoot() {
		return out(HAS_TAGFAMILY_ROOT).has(TagFamilyRootImpl.class).nextOrDefaultExplicit(TagFamilyRootImpl.class, null);
	}

	@Override
	public void setProjectRoot(ProjectRoot projectRoot) {
		linkOut(projectRoot.getImpl(), HAS_PROJECT_ROOT);
	}

	@Override
	public ProjectRoot createProjectRoot() {
		ProjectRootImpl projectRoot = getGraph().addFramedVertex(ProjectRootImpl.class);
		setProjectRoot(projectRoot);
		return projectRoot;
	}

	@Override
	public GroupRoot createGroupRoot() {
		GroupRootImpl groupRoot = getGraph().addFramedVertex(GroupRootImpl.class);
		setGroupRoot(groupRoot);
		return groupRoot;
	}

	@Override
	public RoleRoot createRoleRoot() {
		RoleRootImpl roleRoot = getGraph().addFramedVertex(RoleRootImpl.class);
		setRoleRoot(roleRoot);
		return roleRoot;
	}

	@Override
	public SchemaContainerRoot createRoot() {
		SchemaContainerRootImpl schemaRoot = getGraph().addFramedVertex(SchemaContainerRootImpl.class);
		setSchemaRoot(schemaRoot);
		return schemaRoot;
	}

	@Override
	public UserRoot createUserRoot() {
		UserRootImpl userRoot = getGraph().addFramedVertex(UserRootImpl.class);
		setUserRoot(userRoot);
		return userRoot;
	}

	@Override
	public LanguageRoot createLanguageRoot() {
		LanguageRootImpl languageRoot = getGraph().addFramedVertex(LanguageRootImpl.class);
		setLanguageRoot(languageRoot);
		return languageRoot;
	}

}
