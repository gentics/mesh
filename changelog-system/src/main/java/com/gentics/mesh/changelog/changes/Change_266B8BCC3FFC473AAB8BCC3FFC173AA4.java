package com.gentics.mesh.changelog.changes;

import com.gentics.mesh.changelog.AbstractChange;
import com.gentics.mesh.changelog.MeshGraphHelper;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;

public class Change_266B8BCC3FFC473AAB8BCC3FFC173AA4 extends AbstractChange {

	@Override
	public String getName() {
		return "Fix project release relationship";
	}

	@Override
	public String getDescription() {
		return "Adds a missing edge between project and the release";
	}

	@Override
	public void apply() {
		Vertex meshRoot = MeshGraphHelper.getMeshRootVertex(getGraph());
		Vertex projectRoot = meshRoot.getVertices(Direction.OUT, "HAS_PROJECT_ROOT").iterator().next();
		for (Vertex project : projectRoot.getVertices(Direction.OUT, "HAS_PROJECT")) {
			Vertex releaseRoot = project.getVertices(Direction.OUT, "HAS_RELEASE_ROOT").iterator().next();
			for (Vertex release : releaseRoot.getVertices(Direction.OUT, "HAS_RELEASE")) {
				// Assign the release to the project
				release.addEdge("ASSIGNED_TO_PROJECT", project);
			}
		}
	}
}
