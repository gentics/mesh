package com.gentics.mesh.core.db;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.HibElement;
import com.gentics.mesh.core.db.cluster.ClusterManager;
import com.gentics.mesh.core.rest.admin.cluster.ClusterConfigRequest;
import com.gentics.mesh.core.rest.admin.cluster.ClusterConfigResponse;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.util.ETag;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
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
	 * Shutdown the database. This will terminate the database provider.
	 */
	void shutdown();

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
	 * Checks if the database is empty.
	 *
	 * @return
	 */
	boolean isEmptyDatabase();

	/**
	 * Initialise the database and store the settings.
	 * 
	 * @param meshVersion
	 *            Version of mesh
	 * @param basePaths
	 *            Base paths which will be scanned for graph element classes
	 * @throws Exception
	 */
	void init(String meshVersion, String... basePaths) throws Exception;

	/**
	 * Reload the given mesh element.
	 * 
	 * @param element
	 */
	void reload(HibElement element);

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
	 * Return the list low level change uuids.
	 * 
	 * @return
	 */
	List<String> getChangeUuidList();

	/**
	 * Return the Vert.x instance.
	 * 
	 * @return
	 */
	Vertx vertx();

	/**
	 * Update the cluster configuration.
	 * 
	 * @param request
	 */
	void updateClusterConfig(ClusterConfigRequest request);

	/**
	 * Load the current cluster configuration.
	 * 
	 * @return
	 */
	ClusterConfigResponse loadClusterConfig();

	/**
	 * Block execution if a topology lock was found.
	 */
	void blockingTopologyLockCheck();

	/**
	 * Set the server role to master.
	 */
	void setToMaster();

	/**
	 * Return the global lockable write lock instance.
	 * 
	 * @return
	 */
	WriteLock writeLock();

	/**
	 * Return the cluster manager of the database.
	 * 
	 * @return
	 */
	ClusterManager clusterManager();

	/**
	 * Create a database export.
	 * 
	 * @param outputDirectory
	 * @throws IOException
	 */
	void exportDatabase(String outputDirectory) throws IOException;

	/**
	 * Import an database export
	 * 
	 * @param importFile
	 * @throws IOException
	 */
	void importDatabase(String importFile) throws IOException;

	/**
	 * Create a database backup.
	 * 
	 * @param backupDirectory
	 * @throws IOException
	 * @return The path of the created backup file.
	 */
	String backupDatabase(String backupDirectory) throws IOException;

	/**
	 * Restore a previously created database backup.
	 * 
	 * @param backupFile
	 * @throws IOException
	 */
	void restoreDatabase(String backupFile) throws IOException;

	/**
	 * Generate the database revision change by generating a hash over all database changes and the database vendor version.
	 * 
	 * @return
	 */
	default String getDatabaseRevision() {
		String overrideRev = System.getProperty("mesh.internal.dbrev");
		if (overrideRev != null) {
			return overrideRev;
		}
		StringBuilder builder = new StringBuilder();
		for (String changeUuid : getChangeUuidList()) {
			builder.append(changeUuid);
		}
		return ETag.hash(builder.toString() + getVersion());
	}

	/**
	 * Return the vertex count for the given class.
	 * 
	 * @param clazz
	 * @return
	 */
	long count(Class<? extends HibBaseElement> clazz);

	/**
	 * Return the element version.
	 * 
	 * @param element
	 * @return
	 */
	String getElementVersion(HibElement element);

	/**
	 * Locate all vertices for the given type.
	 * 
	 * @param classOfVertex
	 * @return
	 */
	<T extends HibElement> Iterator<? extends T> getElementsForType(Class<T> classOfVertex);

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
			vertx().executeBlocking(bc -> {
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
			vertx().executeBlocking(bc -> {
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
	 * Executes a supplier in a transaction within the worker thread pool. If the supplier returns null, the maybe is completed, else the value is returned.
	 * 
	 * @param <T>
	 * @param handler
	 * @return
	 */
	default <T> Maybe<T> maybeTx(Function<Tx, T> handler) {
		return maybeTx(handler, false);
	}

	/**
	 * Executes a supplier in a transaction within the worker thread pool. If the supplier returns null, the maybe is completed, else the value is returned.
	 * 
	 * @param handler
	 * @param useWriteLock
	 *            Whether to apply a write lock around the transaction
	 * @param <T>
	 * @return
	 */
	default <T> Maybe<T> maybeTx(Function<Tx, T> handler, boolean useWriteLock) {
		return new io.vertx.reactivex.core.Vertx(vertx()).rxExecuteBlocking(promise -> {
			try {
				if (useWriteLock) {
					T result = null;
					try (WriteLock lock = writeLock().lock(null)) {
						result = tx(handler::apply);
					}
					promise.complete(result);
				} else {
					promise.complete(tx(handler::apply));
				}

			} catch (Throwable e) {
				promise.fail(e);
			}
		}, false);
	}

	/**
	 * Executes a supplier in a transaction within the worker thread pool. If the supplier returns null, a {@link java.util.NoSuchElementException} is emitted.
	 * 
	 * @param handler
	 * @param <T>
	 * @return
	 */
	default <T> Single<T> singleTx(Supplier<T> handler) {
		return maybeTx(tx -> handler.get(), false).toSingle();
	}

	default <T> Single<T> singleTxWriteLock(Function<Tx, T> handler) {
		return maybeTx(handler, true).toSingle();
	}

	/**
	 * Executes an event action in a transaction within the worker thread pool.
	 * @param action
	 * @param <T>
	 * @return
	 */
	default <T> Single<T> singleTxWriteLock(TxEventAction<T> action) {
		AtomicReference<EventQueueBatch> lazyBatch = new AtomicReference<>();
		Function<Tx, T> handler = tx -> {
			EventQueueBatch batch = tx.createBatch();
			lazyBatch.set(batch);
			return action.handle(batch, tx);
		};
		return maybeTx(handler, true).toSingle().doOnSuccess((ignored)-> lazyBatch.get().dispatch());
	}

	/**
	 * Executes a supplier in a transaction within the worker thread pool. If the supplier returns null, a {@link java.util.NoSuchElementException} is emitted.
	 * 
	 * @param handler
	 * @param <T>
	 * @return
	 */
	default <T> Single<T> singleTx(Function<Tx, T> handler) {
		return maybeTx(handler, false).toSingle();
	}

	default <T> Transactional<T> transactional(Function<? super Tx, ? extends T> txFunction) {
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
			public <R> Transactional<R> mapInTx(BiFunction<? super Tx, ? super T, ? extends R> mapper) {
				return transactional(tx -> mapper.apply(tx, txFunction.apply(tx)));
			}

			@Override
			public <R> Transactional<R> flatMap(Function<? super T, Transactional<? extends R>> mapper) {
				return transactional(tx -> {
					T val = txFunction.apply(tx);
					return mapper.apply(val).runInExistingTx(tx);
				});
			}
		};
	}
}
