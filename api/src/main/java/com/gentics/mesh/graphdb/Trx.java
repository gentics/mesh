package com.gentics.mesh.graphdb;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.DelegatingFramedTransactionalGraph;
import com.syncleus.ferma.FramedTransactionalGraph;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class Trx implements AutoCloseable {

	private static final Logger log = LoggerFactory.getLogger(Trx.class);

	private static ThreadLocal<FramedTransactionalGraph> localGraph = new ThreadLocal<>();
	private FramedTransactionalGraph currentGraph;
	private boolean isSuccess = false;
	private FramedTransactionalGraph oldLocalGraph;

	private static boolean debug = false;
	private static CyclicBarrier barrier;

	public Trx(Database database) {
		
		currentGraph = database.startTransaction();
		if (log.isDebugEnabled()) {
			log.debug("Starting transaction {" + currentGraph.hashCode() + "}");
		}
		oldLocalGraph = localGraph.get();
		localGraph.set(currentGraph);
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
		handleDebug();
		if (isSuccess) {
			commit();
		} else {
			rollback();
		}
		setLocalGraph(oldLocalGraph);
	}

	/**
	 * This method is used for testing multithreading issues.
	 */
	private void handleDebug() {
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
		Trx.barrier = barrier;
	}

	public static void enableDebug() {
		Trx.debug = true;
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

	@Deprecated
	public void commit() {
		if (log.isDebugEnabled()) {
			log.debug("Commiting graph {" + currentGraph.hashCode() + "}.");
		}
		long start = System.currentTimeMillis();
		currentGraph.commit();
		long duration = System.currentTimeMillis() - start;
		if (log.isDebugEnabled()) {
			log.debug("Comitting took: " + duration + " [ms]");
		}
	}

	@Deprecated
	public void rollback() {
		if (log.isDebugEnabled()) {
			log.debug("Invoking rollback on graph {" + currentGraph.hashCode() + "}.");
		}
		currentGraph.rollback();
	}

	public static void disableDebug() {
		Trx.debug = false;
	}
}
