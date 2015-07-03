package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.root.impl.MeshRootImpl;

public interface MeshRoot extends MeshVertex {

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

	static MeshRoot getInstance() {
		return MeshRootImpl.getInstance();
	}

}
