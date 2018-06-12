package com.gentics.mesh.changelog.changes;

import static org.apache.tinkerpop.gremlin.structure.Direction.OUT;

import java.util.Iterator;

import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.gentics.mesh.changelog.AbstractChange;
import com.gentics.mesh.changelog.MeshGraphHelper;

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
	public void apply() {
		Vertex meshRoot = MeshGraphHelper.getMeshRootVertex(getGraph());
		Vertex projectRoot = meshRoot.vertices(OUT, "HAS_PROJECT_ROOT").next();
		for (Vertex project : projectRoot.vertices(OUT, "HAS_PROJECT")) {
			Iterator<Vertex> it = project.vertices(OUT, "HAS_RELEASE_ROOT");
			if (it.hasNext()) {
				Vertex releaseRoot = it.next();
				for (Vertex release : releaseRoot.vertices(OUT, "HAS_RELEASE")) {
					// Assign the release to the project
					release.addEdge("ASSIGNED_TO_PROJECT", project);
				}
			}
		}
	}
}
