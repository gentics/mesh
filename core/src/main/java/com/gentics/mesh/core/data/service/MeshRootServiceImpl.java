package com.gentics.mesh.core.data.service;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.model.root.MeshRoot;

@Component
public class MeshRootServiceImpl implements MeshRootService {

	@Override
	public MeshRoot findRoot() {
		//@Query("MATCH (n:MeshRoot) return n")
		return null;
	}

	@Override
	public void save(MeshRoot rootNode) {

	}

	@Override
	public MeshRoot reload(MeshRoot rootNode) {
		return null;
	}

	@Override
	public MeshRoot create() {
		// TODO Auto-generated method stub
		return null;
	}

}
