package com.gentics.mesh.graphdb;

import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.FramedGraph;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class AbstractTrxBase {

	private static final Logger log = LoggerFactory.getLogger(AbstractTrxBase.class);

	/**
	 * Any graph that was found within the thread local is stored here while the new graph is executing. This graph must be restored when the autoclosable.
	 * closes.
	 */
	private FramedGraph oldGraph;

	/**
	 * Graph that is active within the scope of the autoclosable.
	 */
	private FramedGraph currentGraph;

	/**
	 * Initialize the transaction.
	 * 
	 * @param transactionalGraph
	 */
	protected void init(FramedGraph transactionalGraph) {
		// 1. Set the new transactional graph so that it can be accessed via Trx.getGraph()
		setGraph(transactionalGraph);
		if (log.isDebugEnabled()) {
			log.debug("Started transaction {" + getGraph().hashCode() + "}");
		}
		// Handle graph multithreading issues by storing the old graph instance that was found in the threadlocal in a field. 
		setOldGraph(Database.getThreadLocalGraph());
		// Overwrite the current active threadlocal graph with the given transactional graph. This way Ferma graph elements will utilize this instance.
		Database.setThreadLocalGraph(transactionalGraph);
	}

	public FramedGraph getGraph() {
		return currentGraph;
	}

	protected void setGraph(FramedGraph currentGraph) {
		this.currentGraph = currentGraph;
	}

	protected void setOldGraph(FramedGraph oldGraph) {
		this.oldGraph = oldGraph;
	}

	protected FramedGraph getOldGraph() {
		return oldGraph;
	}

	//	/**
	//	 * This method is used for testing multithreading issues.
	//	 */
	//	protected void handleDebug() {
	//		if (Trx.debug && Trx.barrier != null) {
	//			if (log.isDebugEnabled()) {
	//				log.debug("Waiting on trx barrier release..");
	//			}
	//			try {
	//				Trx.barrier.await(10, TimeUnit.SECONDS);
	//				log.debug("Trx barrier released");
	//			} catch (InterruptedException | BrokenBarrierException | TimeoutException e) {
	//				log.error("Trx barrier failed", e);
	//			}
	//		}
	//	}

}
