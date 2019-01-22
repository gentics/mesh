package com.gentics.mesh.changelog.changes;

import static com.tinkerpop.blueprints.Direction.OUT;

import java.util.Iterator;

import com.gentics.mesh.changelog.AbstractChange;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

public class UpdateReleaseSchemaEdge extends AbstractChange {

	@Override
	public String getName() {
		return "Update release schema edges";
	}

	@Override
	public String getDescription() {
		return "Checks whether the release schema edge needs to be active or not";
	}

	@Override
	public void applyInTx() {
		Vertex meshRoot = getMeshRootVertex();
		Vertex projectRoot = meshRoot.getVertices(Direction.OUT, "HAS_PROJECT_ROOT").iterator().next();
		for (Vertex project : projectRoot.getVertices(Direction.OUT, "HAS_PROJECT")) {
			Iterator<Vertex> it = project.getVertices(Direction.OUT, "HAS_RELEASE_ROOT").iterator();
			if (it.hasNext()) {
				Vertex releaseRoot = it.next();
				// Iterate over all releases
				for (Vertex release : releaseRoot.getVertices(Direction.OUT, "HAS_RELEASE")) {
					processRelease(release);
				}
			}
		}

	}

	/**
	 * Add the new properties to the schema and microschema version edges
	 * 
	 * @param release
	 */
	private void processRelease(Vertex release) {
		for (Edge edge : release.getEdges(OUT, "HAS_SCHEMA_VERSION")) {
			edge.setProperty("active", true);
			edge.setProperty("migrationStatus", "COMPLETED");
		}
		for (Edge edge : release.getEdges(OUT, "HAS_MICROSCHEMA_VERSION")) {
			edge.setProperty("active", true);
			edge.setProperty("migrationStatus", "COMPLETED");
		}
	}

	@Override
	public String getUuid() {
		return "988F3FCC0FEB42E48F3FCC0FEB72E49C";
	}

	@Override
	public boolean requiresReindex() {
		return true;
	}

}
