package com.gentics.mesh.changelog.changes;

import java.util.Iterator;

import com.gentics.mesh.changelog.AbstractChange;
import com.gentics.mesh.changelog.MeshGraphHelper;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;

public class ChangeFixReleaseRelationship extends AbstractChange {

	@Override
	public String getUuid() {
		return "266B8BCC3FFC473AAB8BCC3FFC173AA4";
	}

	@Override
	public String getName() {
		return "Fix project release relationship";
	}

	@Override
	public String getDescription() {
		return "Adds a missing edge between project and the release";
	}

	@Override
	public void actualApply() {
		Vertex meshRoot = MeshGraphHelper.getMeshRootVertex(getGraph());
		Vertex projectRoot = meshRoot.getVertices(Direction.OUT, "HAS_PROJECT_ROOT").iterator().next();
		for (Vertex project : projectRoot.getVertices(Direction.OUT, "HAS_PROJECT")) {
			Iterator<Vertex> it = project.getVertices(Direction.OUT, "HAS_RELEASE_ROOT").iterator();
			if (it.hasNext()) {
				Vertex releaseRoot = it.next();
				for (Vertex release : releaseRoot.getVertices(Direction.OUT, "HAS_RELEASE")) {
					// Assign the release to the project
					release.addEdge("ASSIGNED_TO_PROJECT", project);
				}
			}
		}
	}
}
