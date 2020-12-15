package com.gentics.mesh.changelog.changes;

import com.gentics.mesh.changelog.AbstractChange;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

import java.util.Iterator;

import static com.tinkerpop.blueprints.Direction.IN;
import static com.tinkerpop.blueprints.Direction.OUT;

/**
 * Changelog entry which replaces editor edges with properties to reduce contention.
 */
public class NodeContentEditorMigration extends AbstractChange {

	@Override
	public String getName() {
		return "Migrate node content editor";
	}

	@Override
	public String getDescription() {
		return "Replaces the edge between content and editor by a property";
	}

	@Override
	public void applyInTx() {
		long count = 0;
		Iterable<Vertex> it = getGraph().getVertices("@class", "NodeGraphFieldContainerImpl");
		for (Vertex nodeContainer : it) {
			migrateContainer(nodeContainer);
			count++;
			if (count % 1000 == 0) {
				log.info("Migrated {" + count + "} contents");
				getGraph().commit();
			}
		}
	}

	private void migrateContainer(Vertex nodeContainer) {
		Iterator<Edge> it = nodeContainer.getEdges(OUT, "HAS_EDITOR").iterator();
		if (!it.hasNext()) {
			// We skip containers which have no editor set. Those need to be cleaned using the consistency check.
			return;
		}
		Edge editorEdge = it.next();
		String editorUuid = editorEdge
			.getVertex(IN).getProperty("uuid");
		nodeContainer.setProperty("editor", editorUuid);
		editorEdge.remove();
	}

	@Override
	public String getUuid() {
		return "810E8A96A94C42CF8E8A96A94CA2CFD2";
	}
}
