package com.gentics.mesh.changelog.changes;

import static org.apache.tinkerpop.gremlin.structure.Direction.OUT;

import java.util.Iterator;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.gentics.mesh.changelog.AbstractChange;
import com.gentics.mesh.madl.frame.ElementFrame;
import com.gentics.mesh.util.StreamUtil;

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
		try (GraphTraversal<Vertex, Vertex> t = getGraph().traversal().V()) {
			Iterable<Vertex> it = StreamUtil.toIterable(t.has(ElementFrame.TYPE_RESOLUTION_KEY, "NodeGraphFieldContainerImpl"));
			for (Vertex nodeContainer : it) {
				migrateContainer(nodeContainer);
				count++;
				if (count % 1000 == 0) {
					log.info("Migrated {" + count + "} contents");
					getGraph().tx().commit();
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void migrateContainer(Vertex nodeContainer) {
		Iterator<Edge> it = nodeContainer.edges(OUT, "HAS_EDITOR");
		if (!it.hasNext()) {
			// We skip containers which have no editor set. Those need to be cleaned using the consistency check.
			return;
		}
		Edge editorEdge = it.next();
		String editorUuid = editorEdge.inVertex().<String>property("uuid").orElse(null);
		nodeContainer.property("editor", editorUuid);
		editorEdge.remove();
	}

	@Override
	public String getUuid() {
		return "810E8A96A94C42CF8E8A96A94CA2CFD2";
	}
}
