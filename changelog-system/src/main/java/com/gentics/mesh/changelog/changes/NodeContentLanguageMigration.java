package com.gentics.mesh.changelog.changes;

import static com.tinkerpop.blueprints.Direction.IN;
import static com.tinkerpop.blueprints.Direction.OUT;

import java.util.Iterator;

import com.gentics.mesh.changelog.AbstractChange;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

public class NodeContentLanguageMigration extends AbstractChange {

	@Override
	public String getName() {
		return "Migrate node content language";
	}

	@Override
	public String getDescription() {
		return "Replaces the edge between content and language by an property";
	}

	@Override
	public void actualApply() {
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
		Iterator<Edge> it = nodeContainer.getEdges(OUT, "HAS_LANGUAGE").iterator();
		if (!it.hasNext()) {
			throw new RuntimeException("Node content {" + nodeContainer.getId() + "} has no language linked to it.");
		}
		Edge languageEdge = it.next();
		String languageTag = languageEdge
			.getVertex(IN).getProperty("languageTag");
		nodeContainer.setProperty("languageTag", languageTag);
		languageEdge.remove();
	}

	@Override
	public String getUuid() {
		return "28CDB1CA4A6947998DB1CA4A69779973";
	}

}
