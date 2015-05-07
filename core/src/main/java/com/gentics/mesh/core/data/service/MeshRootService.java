package com.gentics.mesh.core.data.service;

import com.gentics.mesh.core.data.model.MeshRoot;

public interface MeshRootService {

	MeshRoot findRoot();

	void save(MeshRoot rootNode);
	
	MeshRoot reload(MeshRoot rootNode);

}
