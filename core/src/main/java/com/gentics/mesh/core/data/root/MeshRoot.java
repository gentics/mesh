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

	SearchQueueRoot getSearchQueueRoot();

	LanguageRoot getLanguageRoot();

	ProjectRoot getProjectRoot();

	SchemaContainerRoot getSchemaContainerRoot();

	TagRoot getTagRoot();

	MeshRootImpl getImpl();

	NodeRoot getNodeRoot();

	TagFamilyRoot getTagFamilyRoot();

	MicroschemaContainerRoot getMicroschemaContainerRoot();

	void clearReferences();
}
