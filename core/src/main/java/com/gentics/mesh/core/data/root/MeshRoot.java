package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.root.impl.MeshRootImpl;

public interface MeshRoot extends MeshVertex {

	static MeshRoot getInstance() {
		return MeshRootImpl.getInstance();
	}

	RoleRoot getRoleRoot();

	GroupRoot getGroupRoot();

	UserRoot getUserRoot();

	LanguageRoot getLanguageRoot();

	ProjectRoot getProjectRoot();

	SchemaContainerRoot getSchemaContainerRoot();

	LanguageRoot createLanguageRoot();

	GroupRoot createGroupRoot();

	UserRoot createUserRoot();

	RoleRoot createRoleRoot();

	ProjectRoot createProjectRoot();

	SchemaContainerRoot createRoot();

	void setGroupRoot(GroupRoot groupRoot);

	MeshRootImpl getImpl();

	NodeRoot getNodeRoot();

	NodeRoot createNodeRoot();

	void setNodeRoot(NodeRoot nodeRoot);

	void setUserRoot(UserRoot userRoot);

	void setRoleRoot(RoleRoot roleRoot);

	void setSchemaRoot(SchemaContainerRoot schemaRoot);

	void setLanguageRoot(LanguageRoot languageRoot);

	TagRoot getTagRoot();

	TagRoot createTagRoot();

	void setProjectRoot(ProjectRoot projectRoot);

	void setTagRoot(TagRoot tagRoot);

}
