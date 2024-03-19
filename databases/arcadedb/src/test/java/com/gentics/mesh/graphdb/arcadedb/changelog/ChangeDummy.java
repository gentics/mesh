package com.gentics.mesh.graphdb.arcadedb.changelog;

import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.gentics.mesh.changelog.AbstractChange;

/**
 * Change which adds fancy moped.
 */
public class ChangeDummy extends AbstractChange {
	
	@Override
	public String getUuid() {
		return "424FA7436B6541269E6CE90C8C3D812D";
	}

	@Override
	public String getName() {
		return "Add fancy moped";
	}

	@Override
	public void applyInTx() {
		Vertex meshRootVertex = getMeshRootVertex();
		Vertex mopedVertex = getGraph().addVertex("TheMoped");
		mopedVertex.property("name", "moped");
		meshRootVertex.addEdge("HAS_MOPED", mopedVertex);
		log.info("Added moped");
	}

	@Override
	public String getDescription() {
		return "Some changes to the graph which add an edge and a vertex";
	}

}
