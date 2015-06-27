package com.gentics.mesh.core.data.model.root;

import com.gentics.mesh.core.data.model.MeshVertex;
import com.gentics.mesh.core.data.model.root.impl.MeshRootImpl;

public interface MeshRoot extends MeshVertex {

	RoleRoot getRoleRoot();

	GroupRoot getGroupRoot();

	UserRoot getUserRoot();

	LanguageRoot getLanguageRoot();

	ProjectRoot getProjectRoot();

	SchemaRoot getSchemaRoot();

	LanguageRoot createLanguageRoot();

	GroupRoot createGroupRoot();

	UserRoot createUserRoot();

	RoleRoot createRoleRoot();

	ProjectRoot createProjectRoot();

	SchemaRoot createRoot();

	void setGroupRoot(GroupRoot groupRoot);

	MeshRootImpl getImpl();

}
