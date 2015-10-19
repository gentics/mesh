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
import io.vertx.core.Vertx;

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
		FramedGraph graph = Database.threadLocalGraph.get();
		if (graph == null) {
			throw new NullPointerException("Could not find thread local graph. Maybe you are executing this code outside of a transaction.");
		}
		return graph;
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
	 * Execute the txHandler within the scope of the no transaction and call the result handler once the transaction handler code has finished.
	 * 
	 * @param txHandler
	 *            Handler that will be executed within the scope of the transaction.
	 * @param resultHandler
	 *            Handler that is being invoked when the transaction has been committed
	 * @return
	 */
	<T> Database trx(TrxHandler<Future<T>> txHandler, Handler<AsyncResult<T>> resultHandler);

	/**
	 * Asynchronously execute the txHandler within the scope of a transaction and invoke the result handler after the transaction code handler finishes or
	 * fails.
	 * 
	 * @param txHandler
	 *            Handler that will be executed within the scope of the transaction.
	 * @param resultHandler
	 * @return
	 */
	<T> Database asyncTrx(TrxHandler<Future<T>> txHandler, Handler<AsyncResult<T>> resultHandler);

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
	<T> Future<T> noTrx(TrxHandler<Future<T>> txHandler);

	/**
	 * Asynchronously execute the txHandler within the scope of a non transaction and invoke the result handler after the transaction code handler finishes.
	 * 
	 * @param txHandler
	 * @param resultHandler
	 * @return
	 */
	<T> Database asyncNoTrx(TrxHandler<Future<T>> txHandler, Handler<AsyncResult<T>> resultHandler);

	/**
	 * Initialize the database and store the settings.
	 * 
	 * @param options
	 * @param vertx
	 */
	void init(StorageOptions options, Vertx vertx);

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

	/**
	 * Adds an edge index for the given label.
	 * 
	 * @param label
	 *            Label for which the edge index should be created
	 * @param extraFields
	 *            Additional fields that should be indexed
	 */
	public void addEdgeIndex(String label, String... extraFields);

	/**
	 * Add an edge index for the given label which only contains the outgoing edge within the index. This is useful for finding all edges which reference the
	 * given vertex via the outgoing edge.
	 * 
	 * @param label
	 */
	void addEdgeIndexSource(String label);

	Object getComposedIndexKey(Object... keys);

	/**
	 * Add an vertex index for the given type of vertex and fields.
	 * 
	 * @param clazzOfVertices
	 * @param fields
	 */
	public void addVertexIndex(Class<?> clazzOfVertices, String... fields);

}
