package com.gentics.mesh.core.data.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.impl.MeshRootImpl;
import com.syncleus.ferma.FramedGraph;

@Component
public class MeshRootService {

	@Autowired
	private FramedGraph fg;

	public MeshRoot findRoot() {
		return fg.v().has(MeshRootImpl.class).nextOrDefault(MeshRootImpl.class, null);
	}

	public MeshRoot create() {
		MeshRootImpl root = fg.addFramedVertex(MeshRootImpl.class);
		return root;
	}

}
