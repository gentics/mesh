package com.gentics.mesh.graphdb;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.syncleus.ferma.FramedGraph;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class AbstractTrx {

	private static CyclicBarrier barrier;

	private static boolean debug = false;

	private static final Logger log = LoggerFactory.getLogger(AbstractTrx.class);

	/**
	 * Thread local that is used to store references to the used graph.
	 */
	private static ThreadLocal<FramedGraph> threadLocalGraph = new ThreadLocal<>();

	/**
	 * Any graph that was found within the thread local is stored here while the new graph is executing. This graph must be restored when the autoclosable.
	 * closes.
	 */
	private FramedGraph oldGraph;

	/**
	 * Graph that is active within the scope of the autoclosable.
	 */
	private FramedGraph currentGraph;

	public static void setThreadLocalGraph(FramedGraph graph) {
		AbstractTrx.threadLocalGraph.set(graph);
	}

	public static FramedGraph getFramedLocalGraph() {
		return getThreadLocalGraph();
	}

	public static FramedGraph getThreadLocalGraph() {
		return AbstractTrx.threadLocalGraph.get();
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

	/**
	 * This method is used for testing multithreading issues.
	 */
	protected void handleDebug() {
		if (debug && barrier != null) {
			if (log.isDebugEnabled()) {
				log.debug("Waiting on trx barrier release..");
			}
			try {
				barrier.await(10, TimeUnit.SECONDS);
				log.debug("Trx barrier released");
			} catch (InterruptedException | BrokenBarrierException | TimeoutException e) {
				log.error("Trx barrier failed", e);
			}
		}
	}

	public static void setBarrier(CyclicBarrier barrier) {
		AbstractTrx.barrier = barrier;
	}

	public static void enableDebug() {
		AbstractTrx.debug = true;
	}

	public static void disableDebug() {
		AbstractTrx.debug = false;
	}
}
