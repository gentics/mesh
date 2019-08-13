package com.gentics.mesh.core.verticle.handler;

import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.rest.event.EventCauseAction.DELETE;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.gentics.madl.tx.TxAction;
import com.gentics.madl.tx.TxAction0;
import com.gentics.madl.tx.TxAction1;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.error.NotModifiedException;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.metric.MetricsService;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.util.ResultInfo;
import com.gentics.mesh.util.Tuple;
import com.gentics.mesh.util.UUIDUtil;

import io.reactivex.Single;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Common request handler methods which can be used for CRUD operations.
 */
@Singleton
public class HandlerUtilities {

	private static final Logger log = LoggerFactory.getLogger(HandlerUtilities.class);

	private Semaphore writeLock = new Semaphore(1);

	private final Database database;
	private final MetricsService metrics;
	private final boolean syncWrites;

	private final Provider<EventQueueBatch> queueProvider;

	private final Provider<BulkActionContext> bulkProvider;

	@Inject
	public HandlerUtilities(Database database, MeshOptions meshOptions, MetricsService metrics, Provider<EventQueueBatch> queueProvider, Provider<BulkActionContext> bulkProvider) {
		this.database = database;
		this.metrics = metrics;
		this.syncWrites = meshOptions.getStorageOptions() != null && meshOptions.getStorageOptions().isSynchronizeWrites();
		this.queueProvider = queueProvider;
		this.bulkProvider = bulkProvider;
	}

	/**
	 * Create an object using the given aggregation node and respond with a transformed object.
	 * 
	 * @param ac
	 * @param handler
	 */
	public <T extends MeshCoreVertex<RM, T>, RM extends RestModel> void createElement(InternalActionContext ac, TxAction1<RootVertex<T>> handler) {
		createOrUpdateElement(ac, null, handler);
	}

	/**
	 * Delete the specified element.
	 * 
	 * @param ac
	 * @param handler
	 *            Handler which provides the root vertex which will be used to load the element
	 * @param uuid
	 *            Uuid of the element which should be deleted
	 */
	public <T extends MeshCoreVertex<RM, T>, RM extends RestModel> void deleteElement(InternalActionContext ac, TxAction1<RootVertex<T>> handler,
		String uuid) {
		lock();
		syncTx(ac, () -> {
			try {
				RootVertex<T> root = handler.handle();
				T element = root.loadObjectByUuid(ac, uuid, DELETE_PERM);

				// Load the name and uuid of the element. We need this info after deletion.
				String elementUuid = element.getUuid();
				bulkableAction(bac -> {
					bac.setRootCause(element.getTypeInfo().getType(), elementUuid, DELETE);
					element.delete(bac);
				});
				log.info("Deleted element {" + elementUuid + "} for type {" + root.getClass().getSimpleName() + "}");
			} finally {
				unlock();
			}
		}, () -> ac.send(NO_CONTENT));

	}

	/**
	 * Locate and update or create the element using the action context data.
	 * 
	 * @param ac
	 * @param uuid
	 *            Uuid of the element which should be updated
	 * @param handler
	 *            Handler which provides the root vertex which should be used when loading the element
	 * 
	 */
	public <T extends MeshCoreVertex<RM, T>, RM extends RestModel> void updateElement(InternalActionContext ac, String uuid,
		TxAction1<RootVertex<T>> handler) {
		createOrUpdateElement(ac, uuid, handler);
	}

	/**
	 * Either create or update an element with the given uuid.
	 * 
	 * @param ac
	 * @param uuid
	 *            Uuid of the element to create or update. If null, an element will be created with random Uuid
	 * @param handler
	 *            Handler which provides the root vertex which should be used when loading the element
	 */
	public <T extends MeshCoreVertex<RM, T>, RM extends RestModel> void createOrUpdateElement(InternalActionContext ac, String uuid,
		TxAction1<RootVertex<T>> handler) {
		lock();
		AtomicBoolean created = new AtomicBoolean(false);
		syncTx(ac, tx -> {
			try {
				RootVertex<T> root = handler.handle();

				// 1. Load the element from the root element using the given uuid (if not null)
				T element = null;
				if (uuid != null) {
					if (!UUIDUtil.isUUID(uuid)) {
						throw error(BAD_REQUEST, "error_illegal_uuid", uuid);
					}
					element = root.loadObjectByUuid(ac, uuid, UPDATE_PERM, false);
				}

				// Check whether we need to update a found element or whether we need to create a new one.
				if (element != null) {
					final T updateElement = element;
					eventAction(batch -> {
						return updateElement.update(ac, batch);
					});
					return updateElement.transformToRestSync(ac, 0);
				} else {
					T createdElement = eventAction(batch -> {
						created.set(true);
						return root.create(ac, batch, uuid);
					});
					RM model = createdElement.transformToRestSync(ac, 0);
					String path = createdElement.getAPIPath(ac);
					ResultInfo info = new ResultInfo(model);
					info.setProperty("path", path);
					createdElement.onCreated();
					ac.setLocation(path);
					return model;
				}
			} finally {
				unlock();
			}
		}, model -> ac.send(model, created.get() ? CREATED : OK));
	}

	/**
	 * Read the element with the given element by loading it from the specified root vertex.
	 * 
	 * @param ac
	 * @param uuid
	 *            Uuid of the element which should be loaded
	 * @param handler
	 *            Handler which provides the root vertex which should be used when loading the element
	 * @param perm
	 *            Permission to check against when loading the element
	 */
	public <T extends MeshCoreVertex<RM, T>, RM extends RestModel> void readElement(InternalActionContext ac, String uuid,
		TxAction1<RootVertex<T>> handler, GraphPermission perm) {

		syncTx(ac, tx -> {
			RootVertex<T> root = handler.handle();
			T element = root.loadObjectByUuid(ac, uuid, perm);

			// Handle etag
			if (ac.getGenericParameters().getETag()) {
				String etag = element.getETag(ac);
				ac.setEtag(etag, true);
				if (ac.matches(etag, true)) {
					throw new NotModifiedException();
				}
			}
			return element.transformToRestSync(ac, 0);
		}, model -> ac.send(model, OK));
	}

	/**
	 * Read a list of elements of the given root vertex and respond with a list response.
	 * 
	 * @param ac
	 * @param handler
	 *            Handler which provides the root vertex which should be used when loading the element
	 */
	public <T extends MeshCoreVertex<RM, T>, RM extends RestModel> void readElementList(InternalActionContext ac, TxAction1<RootVertex<T>> handler) {

		rxSyncTx(ac, tx -> {
			RootVertex<T> root = handler.handle();

			PagingParameters pagingInfo = ac.getPagingParameters();
			TransformablePage<? extends T> page = root.findAll(ac, pagingInfo);

			// Handle etag
			if (ac.getGenericParameters().getETag()) {
				String etag = page.getETag(ac);
				ac.setEtag(etag, true);
				if (ac.matches(etag, true)) {
					throw new NotModifiedException();
				}
			}
			return page.transformToRest(ac, 0);

		}, m -> ac.send(m, OK));
	}

	public <RM> void syncTx(InternalActionContext ac, TxAction<RM> handler, Consumer<RM> action) {
		try {
			RM model = database.tx(handler);
			action.accept(model);
		} catch (Throwable t) {
			ac.fail(t);
		}
	}

	public <RM extends RestModel> void rxSyncTx(InternalActionContext ac, TxAction<Single<RM>> handler, Consumer<RM> action) {
		try {
			Single<RM> model = database.tx(handler);
			model.subscribe(action::accept, ac::fail);
		} catch (Throwable t) {
			ac.fail(t);
		}
	}

	/**
	 * Invoke sync action in a tx.
	 * 
	 * @param ac
	 * @param handler
	 * @param action
	 */
	public <RM extends RestModel> void syncTx(InternalActionContext ac, TxAction0 handler, Runnable action) {
		try {
			database.tx(handler);
			action.run();
		} catch (Throwable t) {
			ac.fail(t);
		}
	}

	/**
	 * Invoke a bulkable action.
	 * 
	 * @param function
	 */
	public void bulkableAction(Consumer<BulkActionContext> function) {
		BulkActionContext b = database.tx(tx -> {
			BulkActionContext bac = bulkProvider.get();
			function.accept(bac);
			return bac;
		});
		b.process(true);
	}

	/**
	 * Invoke a bulkable action.
	 * 
	 * @param function
	 * @return
	 */
	public <T> T bulkableAction(Function<BulkActionContext, T> function) {
		Tuple<T, BulkActionContext> r = database.tx(tx -> {
			BulkActionContext bac = bulkProvider.get();
			T result = function.apply(bac);
			return Tuple.tuple(result, bac);
		});
		r.v2().process(true);
		return r.v1();
	}

	/**
	 * Invoke an event action.
	 * 
	 * @param function
	 */
	public void eventAction(Consumer<EventQueueBatch> function) {
		EventQueueBatch b = database.tx(tx -> {
			EventQueueBatch batch = queueProvider.get();
			function.accept(batch);
			return batch;
		});
		b.dispatch();
	}

	/**
	 * Invoke an event action which returns a result.
	 * 
	 * @param function
	 * @return
	 */
	public <T> T eventAction(Function<EventQueueBatch, T> function) {
		Tuple<T, EventQueueBatch> tuple = database.tx(tx -> {
			EventQueueBatch batch = queueProvider.get();
			T result = function.apply(batch);
			return Tuple.tuple(result, batch);
		});
		tuple.v2().dispatch();
		return tuple.v1();
	}

	/**
	 * Locks writes. Use this to prevent concurrent write transactions.
	 */
	public void lock() {
		if (syncWrites) {
			try {
				writeLock.acquire();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Releases the lock that was acquired in {@link #lock()}.
	 */
	public void unlock() {
		if (syncWrites) {
			writeLock.release();
		}
	}

}
