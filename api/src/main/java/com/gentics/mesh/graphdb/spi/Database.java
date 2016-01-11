package com.gentics.mesh.graphdb.spi;

import static com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException.error;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.io.IOException;
import java.util.Iterator;

import com.gentics.mesh.etc.GraphStorageOptions;
import com.gentics.mesh.graphdb.NoTrx;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.graphdb.model.MeshElement;
import com.syncleus.ferma.FramedGraph;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Vertex;

import io.vertx.core.Vertx;
import rx.Observable;

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
			throw error(INTERNAL_SERVER_ERROR, "Could not find thread local graph. Maybe you are executing this code outside of a transaction.");
		}
		return graph;
	}

	/**
	 * Stop the graph database.
	 */
	void stop();

	/**
	 * Start the graph database.
	 * 
	 * @throws Exception
	 */
	void start() throws Exception;

	/**
	 * Shortcut for stop/start. This will also drop the graph database.
	 * 
	 * @throws Exception
	 */
	void reset() throws Exception;

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
	 * @param trxHandler
	 *            Handler that will be executed within the scope of the transaction.
	 * @return Object which was returned by the handler
	 */
	<T> T trx(TrxHandler<T> trxHandler);

	/**
	 * Asynchronously execute the trxHandler within the scope of a transaction and invoke the result handler after the transaction code handler finishes or
	 * fails.
	 * 
	 * @param trxHandler
	 *            Handler that will be executed within the scope of the transaction.
	 * @return Observable that will emit the object when the transaction has completed
	 */
	<T> Observable<T> asyncTrx(TrxHandler<T> trxHandler);

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
	<T> T noTrx(TrxHandler<T> txHandler);

	/**
	 * Asynchronously execute the trxHandler within the scope of a non transaction.
	 * 
	 * @param trxHandler
	 * @return
	 * @deprecated Use {@link #asyncNoTrxExperimental(TrxHandler)} instead
	 */
	@Deprecated
	<T> Observable<T> asyncNoTrx(TrxHandler<T> trxHandler);

	/**
	 * Asynchronously execute the trxHandler within the scope of a non transaction. Experimental implementation. This version will use RxJava schedulers to
	 * execute the given observable within the scope of a transaction.
	 * 
	 * @param trxHandler
	 * @return
	 */
	<T> Observable<T> asyncNoTrxExperimental(TrxHandler<Observable<T>> trxHandler);

	/**
	 * Initialize the database and store the settings.
	 * 
	 * @param options
	 * @param vertx
	 * @throws Exception
	 */
	void init(GraphStorageOptions options, Vertx vertx) throws Exception;

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
	void addEdgeIndex(String label, String... extraFields);

	/**
	 * Add an edge index for the given label which only contains the outgoing edge within the index. This is useful for finding all edges which reference the
	 * given vertex via the outgoing edge.
	 * 
	 * @param label
	 */
	void addEdgeIndexSource(String label);

	/**
	 * Create a composed index key
	 * 
	 * @param keys
	 * @return
	 */
	Object createComposedIndexKey(Object... keys);

	/**
	 * Add an vertex index for the given type of vertex and fields.
	 * 
	 * @param clazzOfVertices
	 * @param fields
	 */
	void addVertexIndex(Class<?> clazzOfVertices, String... fields);

	void addEdgeType(String label, String... stringPropertyKeys);

	void addVertexType(Class<?> clazzOfVertex);

	Iterator<Vertex> getVertices(Class<?> classOfVertex, String[] fieldNames, Object[] fieldValues);

	void setVertexType(Element element, Class<?> classOfVertex);

}
