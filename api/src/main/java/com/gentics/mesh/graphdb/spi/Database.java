package com.gentics.mesh.graphdb.spi;

import java.io.IOException;

import com.gentics.mesh.etc.StorageOptions;
import com.gentics.mesh.graphdb.NoTrx;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.graphdb.model.MeshElement;
import com.syncleus.ferma.FramedGraph;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

/**
 * Main description of a graph database.
 */
public interface Database {

	/**
	 * Thread local that is used to store references to the used graph.
	 */
	public static ThreadLocal<FramedGraph> threadLocalGraph = new ThreadLocal<>();

	public static void setThreadLocalGraph(FramedGraph graph) {
		Database.threadLocalGraph.set(graph);
	}

	/**
	 * Return the current active graph. A transaction should be the only place where this threadlocal is updated.
	 * 
	 * @return
	 */
	public static FramedGraph getThreadLocalGraph() {
		return Database.threadLocalGraph.get();
	}

	/**
	 * Stop the graph database.
	 */
	void stop();

	/**
	 * Start the graph database.
	 */
	void start();

	/**
	 * Shortcut for stop/start. This will also drop the graph database.
	 */
	void reset();

	/**
	 * Remove all edges and all vertices from the graph.
	 */
	void clear();

	/**
	 * Return a new autoclosable transaction handler. This object should be used within a try-with-resource block.
	 * 
	 * <pre>
	 * {
	 * 	&#64;code
	 * 	try(Trx tx = db.trx()) {
	 * 	  // interact with graph db here
	 *  }
	 * }
	 * </pre>
	 * 
	 * @return
	 */
	@Deprecated
	Trx trx();

	/**
	 * Execute the given handler within the scope of a transaction.
	 * 
	 * @param code
	 * @return
	 */
	<T> Future<T> trx(Handler<Future<T>> code);

	/**
	 * Asynchronously execute the transactionCodeHandler within the scope of a transaction and invoke the result handler after the transaction code handler
	 * finishes or fails.
	 * 
	 * @param transactionCode
	 * @param resultHandler
	 * @return
	 */
	<T> Database asyncTrx(Handler<Future<T>> transactionCode, Handler<AsyncResult<T>> resultHandler);

	/**
	 * Return a autoclosable transaction handler. Please note that this method will return a non transaction handler. All actions invoked are executed atomic
	 * and no rollback can be performed. This object should be used within a try-with-resource block.
	 * 
	 * <pre>
	 * {
	 * 	&#64;code
	 * 	try(NoTrx tx = db.noTrx()) {
	 * 	  // interact with graph db here
	 *  }
	 * }
	 * </pre>
	 * 
	 * @return
	 */
	NoTrx noTrx();

	/**
	 * Execute the given handler within the scope of a no transaction.
	 * 
	 * @param txHandler
	 *            handler that is invoked within the scope of the no-transaction.
	 * @return
	 */
	<T> Future<T> noTrx(Handler<Future<T>> txHandler);

	/**
	 * Execute the txHandler within the scope of the no transaction and call the result handler once the transaction handler code has finished.
	 * 
	 * @param txHandler
	 * @param resultHandler
	 * @return
	 */
	<T> Database trx(Handler<Future<T>> txHandler, Handler<AsyncResult<T>> resultHandler);

	/**
	 * Asynchronously execute the txHandler within the scope of a non transaction and invoke the result handler after the transaction code handler finishes.
	 * 
	 * @param txHandler
	 * @param resultHandler
	 * @return
	 */
	<T> Database asyncNoTrx(Handler<Future<T>> txHandler, Handler<AsyncResult<T>> resultHandler);

	/**
	 * Initialize the database and store the settings.
	 * 
	 * @param options
	 */
	void init(StorageOptions options);

	/**
	 * Reload the given mesh element.
	 * 
	 * @param element
	 */
	void reload(MeshElement element);

	/**
	 * Create a database export.
	 * 
	 * @param outputDirectory
	 * @throws IOException
	 */
	void exportGraph(String outputDirectory) throws IOException;

	/**
	 * Import an database export
	 * 
	 * @param importFile
	 * @throws IOException
	 */
	void importGraph(String importFile) throws IOException;

	/**
	 * Create a database backup.
	 * 
	 * @param backupDirectory
	 * @throws IOException
	 */
	void backupGraph(String backupDirectory) throws IOException;

	/**
	 * Restore a previously created database backup.
	 * 
	 * @param backupFile
	 * @throws IOException
	 */
	void restoreGraph(String backupFile) throws IOException;

}
