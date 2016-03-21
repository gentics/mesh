package com.gentics.mesh.changelog.changes;

import com.gentics.mesh.changelog.AbstractChange;
import com.tinkerpop.blueprints.Vertex;

/**
 * Change which adds fancy moped.
 */
public class Change_424FA7436B6541269E6CE90C8C3D812D extends AbstractChange {

	@Override
	public String getName() {
		return "Add fancy moped";
	}

	@Override
	public void apply() {
		Vertex meshRootVertex = getMeshRootVertex();
		Vertex mopedVertex = getGraph().addVertex("TheMoped");
		mopedVertex.setProperty("name", "moped");
		meshRootVertex.addEdge("HAS_MOPED", mopedVertex);
		log.info("Added moped");
	}

	@Override
	public String getDescription() {
		return "Some changes to the graph which add an edge and a vertex";
	}

}
