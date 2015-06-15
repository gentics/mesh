package com.gentics.mesh.core.data.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.model.root.MeshRoot;
import com.syncleus.ferma.FramedGraph;

@Component
public class MeshRootService  {

	@Autowired
	private FramedGraph framedGraph;

	public MeshRoot findRoot() {
		return framedGraph.v().has("ferma_type", MeshRoot.class.getName()).next(MeshRoot.class);
	}

	public MeshRoot create() {
		MeshRoot root = framedGraph.addFramedVertex(MeshRoot.class);
		return root;
	}

}
