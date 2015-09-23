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

	protected void init(Database database, FramedGraph transactionalGraph) {
		setGraph(transactionalGraph);
		if (log.isDebugEnabled()) {
			log.debug("Started transaction {" + getGraph().hashCode() + "}");
		}
		setOldGraph(Database.getThreadLocalGraph());
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
