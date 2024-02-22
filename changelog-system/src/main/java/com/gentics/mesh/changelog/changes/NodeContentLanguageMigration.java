package com.gentics.mesh.changelog.changes;

import static org.apache.tinkerpop.gremlin.structure.Direction.OUT;

import java.util.Iterator;

import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.gentics.mesh.changelog.AbstractChange;
import com.gentics.mesh.util.StreamUtil;


/**
 * Changelog entry for the content language migration.
 */
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
	public void applyInTx() {
		long count = 0;
		Iterable<Vertex> it = StreamUtil.toIterable(getGraph().vertices("@class", "NodeGraphFieldContainerImpl"));
		for (Vertex nodeContainer : it) {
			migrateContainer(nodeContainer);
			count++;
			if (count % 1000 == 0) {
				log.info("Migrated {" + count + "} contents");
				getGraph().tx().commit();
			}
		}
	}

	private void migrateContainer(Vertex nodeContainer) {
		Iterator<Edge> it = nodeContainer.edges(OUT, "HAS_LANGUAGE");
		if (!it.hasNext()) {
			// We skip containers which have no language set. Those need to be cleaned using the consistency check.
			return;
		}
		Edge languageEdge = it.next();
		String languageTag = languageEdge.inVertex().<String>property("languageTag").orElse(null);
		nodeContainer.property("languageTag", languageTag);
		languageEdge.remove();
	}

	@Override
	public String getUuid() {
		return "28CDB1CA4A6947998DB1CA4A69779973";
	}

}
