package com.gentics.mesh.changelog.changes;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.DefaultGraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.gentics.mesh.changelog.AbstractChange;
import com.gentics.mesh.madl.frame.ElementFrame;

/**
 * Changelog entry which removes the binary edges in order to reduce contention when updating / creating binaries.
 */
public class RemoveBinaryEdges extends AbstractChange {

	@Override
	public String getUuid() {
		return "41827A16E5BE4B45827A16E5BECB450A";
	}

	@Override
	public String getName() {
		return "RemoveBinaryEdges";
	}

	@Override
	public String getDescription() {
		return "Removes binary root and all edges from binary to the binary root.";
	}

	@Override
	public void apply() {
		applyOutsideTx();
		Graph graph = getDb().rawTx();
		setGraph(graph);
		try (DefaultGraphTraversal<?, Vertex> t = new DefaultGraphTraversal<>(getGraph())) {
			t.has(ElementFrame.TYPE_RESOLUTION_KEY, "BinaryRootImpl").forEachRemaining(Element::remove);
			graph.tx().commit();
		} catch (Throwable e) {
			log.error("Invoking rollback due to error", e);
			graph.tx().rollback();
			throw new RuntimeException(e);
		} finally {
			graph.tx().close();
		}
	}
}
