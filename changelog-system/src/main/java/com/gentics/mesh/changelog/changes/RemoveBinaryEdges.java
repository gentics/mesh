package com.gentics.mesh.changelog.changes;

import com.gentics.mesh.changelog.AbstractChange;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.TransactionalGraph;

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
		TransactionalGraph graph = getDb().rawTx();
		setGraph(graph);
		try {
			getGraph().getVertices("@class", "BinaryRootImpl").forEach(Element::remove);
			graph.commit();
		} catch (Throwable e) {
			log.error("Invoking rollback due to error", e);
			graph.rollback();
			throw e;
		} finally {
			graph.shutdown();
		}
		getDb().type().removeVertexType("BinaryRootImpl");
		getDb().type().removeEdgeType("HAS_BINARY_ROOT");
	}
}
