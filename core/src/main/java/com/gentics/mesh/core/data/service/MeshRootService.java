package com.gentics.mesh.core.data.service;

import com.gentics.mesh.core.data.model.root.MeshRoot;

public interface MeshRootService {

	MeshRoot findRoot();

	void save(MeshRoot rootNode);
	
	MeshRoot reload(MeshRoot rootNode);

	MeshRoot create();

}
