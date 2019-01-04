package com.gentics.mesh.changelog.changes;

import java.util.Iterator;

import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.gentics.mesh.changelog.AbstractChange;
import com.syncleus.ferma.typeresolvers.PolymorphicTypeResolver;

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
	public void apply() {

		// Iterate over all field container
		Iterable<Vertex> vertices = getGraph().vertices(PolymorphicTypeResolver.TYPE_RESOLUTION_KEY, "NodeGraphFieldContainerImpl");
		for (Vertex container : vertices) {
			migrateContainer(container);
		}
	}

	private void migrateContainer(Vertex container) {

		boolean isPublished = false;
		Iterator<Edge> edges = container.edges(Direction.IN, "HAS_FIELD_CONTAINER");

		// Check whether the container is published
		for (Edge edge : (Iterable<Edge>) () ->edges) {
			String type = edge.value("edgeType");
			if ("P".equals(type)) {
				isPublished = true;
			}
		}

		// The container is not published anywhere. Remove the bogus publish webroot info which otherwise causes publish webroot conflicts with new versions.
		if (!isPublished) {
			if (container.property(WEBROOT_PUB) != null) {
				log.info("Found inconsistency on container {" + container.property("uuid") + "}");
				container.property(WEBROOT_PUB).remove();
				log.info("Inconsistency fixed");
			}
		}
	}

	@Override
	public String getUuid() {
		return "DE45FC7902A6435585FC7902A64355BB";
	}

}
