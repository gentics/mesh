package com.gentics.mesh.graphdb.spi;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import com.gentics.madl.tx.Tx;
import com.gentics.madl.tx.TxAction;
import com.gentics.madl.tx.TxAction0;
import com.gentics.madl.tx.TxAction1;
import com.gentics.madl.tx.TxFactory;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.cluster.ClusterManager;
import com.gentics.mesh.graphdb.model.MeshElement;
import com.syncleus.ferma.EdgeFrame;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;
import io.vertx.core.AsyncResult;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Main description of a graph database.
 */
public interface Database extends TxFactory {

	Logger log = LoggerFactory.getLogger(Database.class);

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

	/**
	 * Asynchronously execute the given handler within a transaction and return the completable.
	 * 
	 * @param txHandler
	 * @return
	 */
	default Completable asyncTx(TxAction0 txHandler) {
		// Create an exception which we can use to enhance error information in case of timeout or other transaction errors
		final AtomicReference<Exception> reference = new AtomicReference<Exception>(null);
		try {
			throw new Exception("Transaction timeout exception");
		} catch (Exception e1) {
			reference.set(e1);
		}

		return Completable.create(sub -> {

			Mesh.vertx().executeBlocking(bc -> {
				try {
					tx(txHandler);
					bc.complete();
				} catch (Exception e) {
					if (log.isTraceEnabled()) {
						log.trace("Error while handling no-transaction.", e);
					}
					bc.fail(e);
				}
			}, false, done -> {
				if (done.failed()) {
					sub.onError(done.cause());
				} else {
					sub.onComplete();
				}
			});
		});
	}

	/**
	 * Executes the given action in a worker pool thread and returns a single which can be subscribed to get the result.
	 * 
	 * @param trxHandler
	 * @return
	 */
	default <T> Single<T> asyncTx(TxAction1<Single<T>> trxHandler) {
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
							T ele = result.timeout(40, TimeUnit.SECONDS).blockingGet();
							bc.complete(ele);
						} catch (Exception e2) {
							if (e2 instanceof TimeoutException) {
								log.error("Timeout while processing result of transaction handler.", e2);
								log.error("Calling transaction stacktrace.", reference.get());
								bc.fail(reference.get());
							} else {
								throw e2;
							}
						}
					}
				} catch (Exception e) {
					if (log.isTraceEnabled()) {
						log.trace("Error while handling no-transaction.", e);
					}
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
	default <T> Single<T> asyncTx(TxAction<Single<T>> trxHandler) {
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
							T ele = result.timeout(40, TimeUnit.SECONDS).blockingGet();
							bc.complete(ele);
						} catch (Exception e2) {
							if (e2 instanceof TimeoutException) {
								log.error("Timeout while processing result of transaction handler.", e2);
								log.error("Calling transaction stacktrace.", reference.get());
								bc.fail(reference.get());
							} else {
								throw e2;
							}
						}
					}
				} catch (Exception e) {
					if (!(e instanceof GenericRestException)) {
						log.error("Error while handling no-transaction.", e);
					}
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
	 * Find the vertex with the given key/value setup. Indices which provide this information will automatically be utilized.
	 * 
	 * @param propertyKey
	 * @param propertyValue
	 * @param clazz
	 * @return Found element or null if no element was found
	 */
	<T extends MeshElement> T findVertex(String propertyKey, Object propertyValue, Class<T> clazz);

	/**
	 * Find the edge with the given key/value setup. Indices which provide this information will automatically be utilized.
	 * 
	 * @param propertyKey
	 * @param propertyValue
	 * @param clazz
	 * @return Found element or null if no element was found
	 */
	<T extends EdgeFrame> T findEdge(String propertyKey, Object propertyValue, Class<T> clazz);

	/**
	 * Generate the database revision change by generating a hash over all database changes and the database vendor version.
	 * 
	 * @return
	 */
	String getDatabaseRevision();

	/**
	 * Return the element version.
	 * 
	 * @param vertex
	 * @return
	 */
	String getElementVersion(Element element);

	void shutdown();

	/**
	 * Reload the given element.
	 * 
	 * @param element
	 */
	void reload(Element element);

	default <T> Transactional<T> transactional(Function<Tx, T> txFunction) {
		return new Transactional<T>() {
			@Override
			public T runInExistingTx(Tx tx) {
				try {
					return txFunction.apply(tx);
				} catch (Exception e) {
					// TODO Maybe use other Exception
					throw new RuntimeException(e);
				}
			}

			@Override
			public T runInNewTx() {
				return tx(this::runInExistingTx);
			}

			// @Override
			// public <R> Transactional<R> map(Function<T, R> mapper) {
			// // TODO run map outside Tx (needs MappingTransactional)
			// return transactional(tx -> mapper.apply(txFunction.apply(tx)));
			// }

			@Override
			public <R> Transactional<R> mapInTx(BiFunction<Tx, T, R> mapper) {
				return transactional(tx -> mapper.apply(tx, txFunction.apply(tx)));
			}

			@Override
			public <R> Transactional<R> flatMap(Function<T, Transactional<R>> mapper) {
				return transactional(tx -> {
					T val = txFunction.apply(tx);
					return mapper.apply(val).runInExistingTx(tx);
				});
			}
		};
	}

	/*
	 * Return the type handler for the database.
	 */
	TypeHandler type();

	/**
	 * Return the index handler for the database.
	 * 
	 * @return
	 */
	IndexHandler index();

	/**
	 * Return the cluster manager of the database.
	 * 
	 * @return
	 */
	ClusterManager clusterManager();

	/**
	 * Use index()
	 */
	@Deprecated
	default Object createComposedIndexKey(Object... keys) {
		return index().createComposedIndexKey(keys);
	}

	List<String> getChangeUuidList();
}
