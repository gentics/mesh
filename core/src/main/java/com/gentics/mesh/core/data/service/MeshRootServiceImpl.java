package com.gentics.mesh.core.data.service;

import org.jglue.totorom.FramedGraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.model.root.MeshRoot;

@Component
public class MeshRootServiceImpl implements MeshRootService {

	@Autowired
	private FramedGraph framedGraph;

	@Override
	public MeshRoot findRoot() {
		return framedGraph.V().has("java_class", MeshRoot.class.getName()).next(MeshRoot.class);
	}

	@Override
	public MeshRoot create() {
		MeshRoot root = framedGraph.addVertex(MeshRoot.class);
		return root;
	}

}
