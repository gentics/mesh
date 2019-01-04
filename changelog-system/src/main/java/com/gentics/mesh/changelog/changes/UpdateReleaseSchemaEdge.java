package com.gentics.mesh.changelog.changes;

import static org.apache.tinkerpop.gremlin.structure.Direction.OUT;

import java.util.Iterator;

import com.gentics.mesh.changelog.AbstractChange;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;

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
	public void apply() {
		Vertex meshRoot = getMeshRootVertex();
		Vertex projectRoot = meshRoot.vertices(Direction.OUT, "HAS_PROJECT_ROOT").next();
		for (Vertex project : (Iterable<Vertex>) () -> projectRoot.vertices(Direction.OUT, "HAS_PROJECT")) {
			Iterator<Vertex> it = project.vertices(Direction.OUT, "HAS_RELEASE_ROOT");
			if (it.hasNext()) {
				Vertex releaseRoot = it.next();
				// Iterate over all releases
				for (Vertex release : (Iterable<Vertex>) () -> releaseRoot.vertices(Direction.OUT, "HAS_RELEASE")) {
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
		for (Edge edge : (Iterable<Edge>) () -> release.edges(OUT, "HAS_SCHEMA_VERSION")) {
			edge.property("active", true);
			edge.property("migrationStatus", "COMPLETED");
		}
		for (Edge edge : (Iterable<Edge>) () -> release.edges(OUT, "HAS_MICROSCHEMA_VERSION")) {
			edge.property("active", true);
			edge.property("migrationStatus", "COMPLETED");
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
