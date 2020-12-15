package com.gentics.mesh.changelog.changes;

import com.gentics.mesh.changelog.AbstractChange;
import com.syncleus.ferma.ElementFrame;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

/**
 * Changelog entry which removes bogus properties.
 */
public class RemoveBogusWebrootProperty extends AbstractChange {

	public static final String WEBROOT_PUB = "publishedWebrootPathInfo";

	@Override
	public String getName() {
		return "Fix data inconsistency for older versions.";
	}

	@Override
	public String getDescription() {
		return "Remove the publish webroot information from older versions";
	}

	@Override
	public void applyInTx() {

		// Iterate over all field container
		Iterable<Vertex> vertices = getGraph().getVertices(ElementFrame.TYPE_RESOLUTION_KEY, "NodeGraphFieldContainerImpl");
		for (Vertex container : vertices) {
			migrateContainer(container);
		}
	}

	private void migrateContainer(Vertex container) {

		boolean isPublished = false;
		Iterable<Edge> edges = container.getEdges(Direction.IN, "HAS_FIELD_CONTAINER");

		// Check whether the container is published
		for (Edge edge : edges) {
			String type = edge.getProperty("edgeType");
			if ("P".equals(type)) {
				isPublished = true;
			}
		}

		// The container is not published anywhere. Remove the bogus publish webroot info which otherwise causes publish webroot conflicts with new versions.
		if (!isPublished) {
			if (container.getProperty(WEBROOT_PUB) != null) {
				log.info("Found inconsistency on container {" + container.getProperty("uuid") + "}");
				container.removeProperty(WEBROOT_PUB);
				log.info("Inconsistency fixed");
			}
		}
	}

	@Override
	public String getUuid() {
		return "DE45FC7902A6435585FC7902A64355BB";
	}

}
