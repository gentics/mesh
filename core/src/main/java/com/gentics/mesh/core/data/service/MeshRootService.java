package com.gentics.mesh.core.data.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.model.root.MeshRoot;
import com.gentics.mesh.util.TraversalHelper;
import com.syncleus.ferma.FramedGraph;

@Component
public class MeshRootService {

	@Autowired
	private FramedGraph fg;

	public MeshRoot findRoot() {
		return TraversalHelper.nextExplicitOrNull(fg.v().has(MeshRoot.class), MeshRoot.class);
	}

	public MeshRoot create() {
		MeshRoot root = fg.addFramedVertex(MeshRoot.class);
		return root;
	}

}
