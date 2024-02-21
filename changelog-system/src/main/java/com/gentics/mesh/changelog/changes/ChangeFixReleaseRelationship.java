package com.gentics.mesh.changelog.changes;

import java.util.Iterator;

import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.gentics.mesh.changelog.AbstractChange;
import com.gentics.mesh.changelog.MeshGraphHelper;
import com.gentics.mesh.util.StreamUtil;


/**
 * Change which fixes the release edges in the graph.
 */
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
	public void applyInTx() {
		Vertex meshRoot = MeshGraphHelper.getMeshRootVertex(getGraph());
		Vertex projectRoot = meshRoot.vertices(Direction.OUT, "HAS_PROJECT_ROOT").next();
		for (Vertex project : StreamUtil.toIterable(projectRoot.vertices(Direction.OUT, "HAS_PROJECT"))) {
			Iterator<Vertex> it = project.vertices(Direction.OUT, "HAS_RELEASE_ROOT");
			if (it.hasNext()) {
				Vertex releaseRoot = it.next();
				for (Vertex release : StreamUtil.toIterable(releaseRoot.vertices(Direction.OUT, "HAS_RELEASE"))) {
					// Assign the release to the project
					release.addEdge("ASSIGNED_TO_PROJECT", project);
				}
			}
		}
	}
}
