package com.gentics.mesh.core.data.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.model.root.MeshRoot;
import com.syncleus.ferma.FramedGraph;

@Component
public class MeshRootService {

	@Autowired
	private FramedGraph fg;

	public MeshRoot findRoot() {
		return fg.v().has(MeshRoot.class).nextOrDefault(MeshRoot.class, null);
	}

	public MeshRoot create() {
		MeshRoot root = fg.addFramedVertex(MeshRoot.class);
		return root;
	}

}
