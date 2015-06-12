package com.gentics.mesh.core.data.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.model.root.MeshRoot;
import com.syncleus.ferma.FramedGraph;

@Component
public class MeshRootServiceImpl implements MeshRootService {

	@Autowired
	private FramedGraph framedGraph;

	@Override
	public MeshRoot findRoot() {
//		for (Vertex vertex : framedGraph.getVertices()) {
//			for (String key : vertex.getPropertyKeys()) {
//				System.out.println("Key:" + key + " Value:" + vertex.getProperty(key));
//			}
//		}
		return framedGraph.v().has("ferma_type", MeshRoot.class.getName()).next(MeshRoot.class);
	}

	@Override
	public MeshRoot create() {
		MeshRoot root = framedGraph.addFramedVertex(MeshRoot.class);
		return root;
	}

}
