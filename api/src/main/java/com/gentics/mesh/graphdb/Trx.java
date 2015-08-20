package com.gentics.mesh.graphdb;

import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.DelegatingFramedTransactionalGraph;
import com.syncleus.ferma.FramedTransactionalGraph;
import com.syncleus.ferma.VertexFrame;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class Trx implements AutoCloseable {

	private static final Logger log = LoggerFactory.getLogger(Trx.class);

	private static ThreadLocal<FramedTransactionalGraph> localGraph = new ThreadLocal<>();
	private FramedTransactionalGraph currentGraph;
	private boolean isSuccess = false;
	private FramedTransactionalGraph oldLocalGraph;

	public Trx(Database database) {
		currentGraph = new DelegatingFramedTransactionalGraph<>(database.getFramedGraph().newTransaction(), true, false);
		if (log.isDebugEnabled()) {
			log.debug("Starting transaction {" + currentGraph.hashCode() + "}");
		}
		oldLocalGraph = localGraph.get();
		//if (oldLocalGraph == null) {
		localGraph.set(currentGraph);
		//} else {
		//			currentGraph = localGraph.get();
		//}
	}

	public void success() {
		isSuccess = true;
	}

	public void failure() {
		isSuccess = false;
	}

	public FramedTransactionalGraph getGraph() {
		return currentGraph;
	}

	@Override
	public void close() {
		if (isSuccess) {
			if (log.isDebugEnabled()) {
				log.debug("Commiting graph {" + currentGraph.hashCode() + "}.");
			}
			currentGraph.commit();
		} else {
			if (log.isDebugEnabled()) {
				log.debug("Invoking rollback on graph {" + currentGraph.hashCode() + "}.");
			}
			currentGraph.rollback();
		}
		setLocalGraph(oldLocalGraph);
	}

	public static void setLocalGraph(FramedTransactionalGraph graph) {
		Trx.localGraph.set(graph);
	}

	public static FramedTransactionalGraph getFramedLocalGraph() {
		return getLocalGraph();
	}

	public static FramedTransactionalGraph getLocalGraph() {
		return Trx.localGraph.get();
	}
}
