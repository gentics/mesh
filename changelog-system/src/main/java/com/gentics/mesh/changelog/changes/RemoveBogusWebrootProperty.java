package com.gentics.mesh.changelog.changes;

import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.gentics.mesh.changelog.AbstractChange;
import com.gentics.mesh.madl.frame.ElementFrame;
import com.gentics.mesh.util.StreamUtil;


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
		Iterable<Vertex> vertices = StreamUtil.toIterable(getGraph().vertices(ElementFrame.TYPE_RESOLUTION_KEY, "NodeGraphFieldContainerImpl"));
		for (Vertex container : vertices) {
			migrateContainer(container);
		}
	}

	private void migrateContainer(Vertex container) {

		boolean isPublished = false;
		Iterable<Edge> edges = StreamUtil.toIterable(container.edges(Direction.IN, "HAS_FIELD_CONTAINER"));

		// Check whether the container is published
		for (Edge edge : edges) {
			String type = edge.<String>property("edgeType").orElse(null);
			if ("P".equals(type)) {
				isPublished = true;
			}
		}

		// The container is not published anywhere. Remove the bogus publish webroot info which otherwise causes publish webroot conflicts with new versions.
		if (!isPublished) {
			if (container.property(WEBROOT_PUB).orElse(null) != null) {
				log.info("Found inconsistency on container {" + container.property("uuid").orElse(null) + "}");
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
