package com.gentics.mesh.graphdb.spi;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.rest.admin.cluster.ClusterStatusResponse;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.model.MeshElement;
import com.syncleus.ferma.tx.Tx;
import com.syncleus.ferma.tx.TxAction;
import com.syncleus.ferma.tx.TxAction1;
import com.syncleus.ferma.tx.TxFactory;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;

import io.vertx.core.AsyncResult;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Single;

/**
 * Main description of a graph database.
 */
public interface Database extends TxFactory {

	static final Logger log = LoggerFactory.getLogger(Database.class);

	/**
	 * Stop the graph database.
	 */
	void stop();

	/**
	 * Start the graph database.
	 * 
	 * @throws Exception
	 */
	void setupConnectionPool() throws Exception;

	/**
	 * Close the pool and thus stop the graph database.
	 */
	void closeConnectionPool();

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

	default <T> Single<T> operateTx(TxAction1<Single<T>> trxHandler) {
		// Create an exception which we can use to enhance error information in case of timeout or other transaction errors
		final AtomicReference<Exception> reference = new AtomicReference<Exception>(null);
		try {
			throw new Exception("Transaction timeout exception");
		} catch (Exception e1) {
			reference.set(e1);
		}

		return Single.create(sub -> {
			Mesh.vertx().executeBlocking(bc -> {
				try (Tx tx = tx()) {
					Single<T> result = trxHandler.handle();
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
	 * Asynchronously execute the trxHandler within the scope of a non transaction.
	 * 
	 * @param trxHandler
	 * @return
	 */
	default <T> Single<T> operateTx(TxAction<Single<T>> trxHandler) {
		// Create an exception which we can use to enhance error information in case of timeout or other transaction errors
		final AtomicReference<Exception> reference = new AtomicReference<Exception>(null);
		try {
			throw new Exception("Transaction timeout exception");
		} catch (Exception e1) {
			reference.set(e1);
		}

		return Single.create(sub -> {
			Mesh.vertx().executeBlocking(bc -> {
				try (Tx tx = tx()) {
					Single<T> result = trxHandler.handle(tx);
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
	 * Initialise the database and store the settings.
	 * 
	 * @param options
	 *            Mesh options
	 * @param meshVersion
	 *            Version of mesh
	 * @param basePaths
	 *            Base paths which will be scanned for graph element classes
	 * @throws Exception
	 */
	void init(MeshOptions options, String meshVersion, String... basePaths) throws Exception;

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
	 * @param fieldKey
	 */
	default void addVertexIndex(Class<?> clazzOfVertices, boolean unique, String fieldKey, FieldType fieldType) {
		addVertexIndex(clazzOfVertices.getSimpleName(), clazzOfVertices, unique, fieldKey, fieldType);
	}

	/**
	 * Add a named vertex index for the given type of vertex and fields.
	 * 
	 * @param indexName
	 *            index name
	 * @param clazzOfVertices
	 * @param unique
	 * @param fieldKey
	 */
	void addVertexIndex(String indexName, Class<?> clazzOfVertices, boolean unique, String fieldKey, FieldType fieldType);

	/**
	 * Check whether the values can be put into the given index for the given element.
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
	 * Check whether the value can be put into the given index for a new element of given class.
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
	 * Remove the vertex type with the given name.
	 * 
	 * @param string
	 */
	void removeVertexType(String typeName);

	/**
	 * Remove the edge type with the given name.
	 * 
	 * @param typeName
	 */
	void removeEdgeType(String typeName);

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
	 * Locate all vertices for the given type.
	 * 
	 * @param classOfVertex
	 * @return
	 */
	<T extends MeshVertex> Iterator<? extends T> getVerticesForType(Class<T> classOfVertex);

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

	/**
	 * Perform an edge SB-Tree index lookup. This method will load the index for the given edge label and postfix and return a list of all inbound vertex ids
	 * for the found edges. The key defines the outbound edge vertex id which is used to filter the edges.
	 * 
	 * @param edgeLabel
	 * @param indexPostfix
	 * @param key
	 *            outbound vertex id of the edge to be checked
	 * @return List of found inbound vertex ids for the found edges
	 */
	List<Object> edgeLookup(String edgeLabel, String indexPostfix, Object key);

	/**
	 * Join the cluster and block until the graph database has been received.
	 * 
	 * @throws InterruptedException
	 */
	void joinCluster() throws InterruptedException;

	/**
	 * Start the graph database server which will provide cluster support.
	 * 
	 * @throws Exception
	 */
	void startServer() throws Exception;

	Object getHazelcast();

	/**
	 * Return the database cluster status.
	 * 
	 * @return
	 */
	ClusterStatusResponse getClusterStatus();

	/**
	 * Find the vertex with the given key/value setup. Indices which provide this information will automatically be utilized.
	 * 
	 * @param propertyKey
	 * @param propertyValue
	 * @param clazz
	 * @return
	 */
	<T extends MeshElement> T findVertex(String propertyKey, Object propertyValue, Class<T> clazz);

}
