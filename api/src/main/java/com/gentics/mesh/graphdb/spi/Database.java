package com.gentics.mesh.graphdb.spi;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.etc.GraphStorageOptions;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.graphdb.Tx;
import com.gentics.mesh.graphdb.model.MeshElement;
import com.syncleus.ferma.FramedGraph;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;

import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Single;

/**
 * Main description of a graph database.
 */
public interface Database {

	static final Logger log = LoggerFactory.getLogger(Database.class);

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
	 * Return a new autocloseable transaction handler. This object should be used within a try-with-resource block.
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
	Tx tx();

	/**
	 * Execute the txHandler within the scope of the no transaction and call the result handler once the transaction handler code has finished.
	 * 
	 * @param txHandler
	 *            Handler that will be executed within the scope of the transaction.
	 * @return Object which was returned by the handler
	 */
	<T> T tx(TxHandler<T> txHandler);

	/**
	 * Return a autocloseable transaction handler. Please note that this method will return a non transaction handler. All actions invoked are executed atomic
	 * and no rollback can be performed. This object should be used within a try-with-resource block.
	 * 
	 * <pre>
	 * {
	 * 	&#64;code
	 * 	try(NoTx tx = db.noTx()) {
	 * 	  // interact with graph db here
	 *  }
	 * }
	 * </pre>
	 * 
	 * @return
	 */
	NoTx noTx();

	/**
	 * Asynchronously execute the trxHandler within the scope of a non transaction.
	 * 
	 * @param trxHandler
	 * @return
	 */
	default <T> Single<T> operateNoTx(TxHandler<Single<T>> trxHandler) {
		// Create an exception which we can use to enhance error information in case of timeout or other tranaction errors
		final AtomicReference<Exception> reference = new AtomicReference<Exception>(null);
		try {
			throw new Exception("Transaction timeout exception");
		} catch (Exception e1) {
			reference.set(e1);
		}

		return Single.create(sub -> {
			Mesh.vertx().executeBlocking(bc -> {
				try (NoTx noTx = noTx()) {
					Single<T> result = trxHandler.call();
					if (result == null) {
						bc.complete();
					} else {
						try {
							T ele = result.toBlocking().toFuture().get(40, TimeUnit.SECONDS);
							bc.complete(ele);
						} catch (TimeoutException e2) {
							log.error("Timeout while processing result of transaction handler.", e2);
							log.error("Calling transaction stacktrace.", reference.get());
							bc.fail(reference.get());
						}
					}
				} catch (Exception e) {
					log.error("Error while handling no-transaction.", e);
					bc.fail(e);
				}
			}, false, (AsyncResult<T> done) -> {
				if (done.failed()) {
					sub.onError(done.cause());
				} else {
					sub.onSuccess(done.result());
				}
			});
		});
	}

	/**
	 * Execute the given handler within the scope of a no transaction.
	 * 
	 * @param txHandler
	 *            handler that is invoked within the scope of the no-transaction.
	 * @return
	 */
	<T> T noTx(TxHandler<T> txHandler);

	/**
	 * Initialise the database and store the settings.
	 * 
	 * @param options
	 *            Graph database options
	 * @param vertx
	 *            Vertx instance used to execute blocking code
	 * @param basePaths
	 *            Base paths which will be scanned for graph element classes
	 * @throws Exception
	 */
	void init(GraphStorageOptions options, Vertx vertx, String... basePaths) throws Exception;

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
	 * Add an edge index for the given label.
	 * 
	 * @param label
	 *            Label for which the edge index should be created
	 * @param includeInOut
	 *            If set to true the in/out information will be added to the edge index with postfix _inout
	 * @param includeIn
	 *            If set to true the in information will be added to the edge index with postfix _in
	 * @param includeOut
	 *            If set to true the in information will be added to the edge index with postfix _out
	 * @param extraFields
	 *            Additional fields that should be indexed. All fields will be to an index with postfix _extra.
	 */
	void addEdgeIndex(String label, boolean includeInOut, boolean includeIn, boolean includeOut, String... extraFields);

	/**
	 * Add an edge index for the given label.
	 * 
	 * @param label
	 * @param extraFields
	 *            Additional fields that should be indexed. All fields will be to an index with postfix _extra.
	 */
	default void addEdgeIndex(String label, String... extraFields) {
		addEdgeIndex(label, false, false, false);
	}

	/**
	 * Add edge index for the given fields.
	 * 
	 * The index name will be constructed using the label and the index postfix (e.g: has_node_postfix)
	 * 
	 * @param label
	 * @param indexPostfix
	 *            postfix of the index
	 * @param fields
	 */
	void addCustomEdgeIndex(String label, String indexPostfix, String... fields);

	/**
	 * Create a composed index key
	 * 
	 * @param keys
	 * @return
	 */
	Object createComposedIndexKey(Object... keys);

	/**
	 * Add a vertex index for the given type of vertex and fields.
	 * 
	 * @param clazzOfVertices
	 * @param unique
	 *            true to create unique key
	 * @param fields
	 */
	default void addVertexIndex(Class<?> clazzOfVertices, boolean unique, String... fields) {
		addVertexIndex(clazzOfVertices.getSimpleName(), clazzOfVertices, unique, fields);
	}

	/**
	 * Add a named vertex index for the given type of vertex and fields
	 * 
	 * @param indexName
	 *            index name
	 * @param clazzOfVertices
	 * @param unique
	 * @param fields
	 */
	void addVertexIndex(String indexName, Class<?> clazzOfVertices, boolean unique, String... fields);

	/**
	 * Check whether the values can be put into the given index for the given element
	 * 
	 * @param indexName
	 *            index name
	 * @param element
	 *            element
	 * @param key
	 *            index key to check
	 * @return the conflicting element or null if no conflict exists
	 */
	<T extends MeshElement> T checkIndexUniqueness(String indexName, T element, Object key);

	/**
	 * Check whether the value can be put into the given index for a new element of given class
	 * 
	 * @param indexName
	 *            index name
	 * @param classOfT
	 *            class of the proposed new element
	 * @param key
	 *            index key to check
	 * @return the conflicting element or null if no conflict exists
	 */
	<T extends MeshElement> T checkIndexUniqueness(String indexName, Class<T> classOfT, Object key);

	/**
	 * Create a new edge type for the given label.
	 * 
	 * @param label
	 * @param superClazzOfEdge
	 * @param stringPropertyKeys
	 */
	void addEdgeType(String label, Class<?> superClazzOfEdge, String... stringPropertyKeys);

	/**
	 * Create a new edge type for the given label.
	 * 
	 * @param label
	 * @param stringPropertyKeys
	 */
	void addEdgeType(String label, String... stringPropertyKeys);

	/**
	 * Create a new vertex type for the given vertex class type.
	 * 
	 * @param clazzOfVertex
	 * @param superClazzOfVertex
	 *            Super vertex type. If null "V" will be used.
	 */
	void addVertexType(Class<?> clazzOfVertex, Class<?> superClazzOfVertex);

	/**
	 * Utilize the index and locate the matching vertices.
	 * 
	 * @param classOfVertex
	 * @param fieldNames
	 * @param fieldValues
	 * @return
	 */
	Iterator<Vertex> getVertices(Class<?> classOfVertex, String[] fieldNames, Object[] fieldValues);

	/**
	 * Update the vertex type for the given element using the class type.
	 * 
	 * @param element
	 * @param classOfVertex
	 */
	void setVertexType(Element element, Class<?> classOfVertex);

	/**
	 * Get the underlying raw transaction.
	 * 
	 * @return
	 */
	TransactionalGraph rawTx();

	/**
	 * Return the vendor name.
	 * 
	 * @return
	 */
	String getVendorName();

	/**
	 * Return the database product version.
	 * 
	 * @return
	 */
	String getVersion();

	/**
	 * Configure the current transaction that way that mass inserts can be performed.
	 */
	void enableMassInsert();

	/**
	 * Reset a previously set intent.
	 */
	void resetIntent();

	/**
	 * Tell the graph database that a mass insert will follow.
	 */
	void setMassInsertIntent();

}
