package com.gentics.mesh.core.verticle.handler;

import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.tuple.Triple;
import org.elasticsearch.common.collect.Tuple;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.IndexableElement;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.error.NotModifiedException;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.graphdb.spi.TxHandler;
import com.gentics.mesh.parameter.impl.PagingParameters;

import io.vertx.core.AsyncResult;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Single;
import rx.functions.Action1;

public final class HandlerUtilities {

	private static final Logger log = LoggerFactory.getLogger(HandlerUtilities.class);

	/**
	 * Create an object using the given aggregation node and respond with a transformed object.
	 * 
	 * @param ac
	 * @param handler
	 */
	public static <T extends MeshCoreVertex<RM, T>, RM extends RestModel> void createElement(InternalActionContext ac,
			TxHandler<RootVertex<T>> handler) {
		operateNoTx(ac, () -> {
			Database db = MeshInternal.get().database();
			Triple<RM, String, SearchQueueBatch> info = db.tx(() -> {
				SearchQueue queue = MeshInternal.get().boot().meshRoot().getSearchQueue();
				RootVertex<T> root = handler.call();
				SearchQueueBatch batch = queue.createBatch();

				T created = root.create(ac, batch);
				RM model = created.transformToRestSync(ac, 0);
				String path = created.getAPIPath(ac);
				return Triple.of(model, path, batch);
			});

			RM model = info.getLeft();
			String path = info.getMiddle();
			SearchQueueBatch batch = info.getRight();
			ac.setLocation(path);
			// TODO don't wait forever in order to prevent locking the thread
			batch.process().await();
			return model;
		}, model -> ac.send(model, CREATED));

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
	public static <T extends MeshCoreVertex<RM, T>, RM extends RestModel> void deleteElement(InternalActionContext ac,
			TxHandler<RootVertex<T>> handler, String uuid) {
		operateNoTx(ac, () -> {
			Database db = MeshInternal.get().database();
			RootVertex<T> root = handler.call();
			T element = root.loadObjectByUuid(ac, uuid, DELETE_PERM);
			String elementUuid = element.getUuid();
			SearchQueueBatch sqb = db.tx(() -> {

				// Check whether the element is indexable. Indexable elements must also be purged from the search index.
				if (element instanceof IndexableElement) {
					SearchQueue queue = MeshInternal.get().boot().meshRoot().getSearchQueue();
					SearchQueueBatch batch = queue.createBatch();
					element.delete(batch);
					return batch;
				} else {
					throw error(INTERNAL_SERVER_ERROR, "Could not determine object name");
				}
			});
			log.info("Deleted element {" + elementUuid + "}");
			return sqb.process().andThen(Single.just((RM) null)).toBlocking().value();
		}, model -> ac.send(NO_CONTENT));
	}

	/**
	 * Locate and update the element using the action context data.
	 * 
	 * @param ac
	 * @param uuid
	 *            Uuid of the element which should be updated
	 * @param handler
	 *            Handler which provides the root vertex which should be used when loading the element
	 * 
	 */
	public static <T extends MeshCoreVertex<RM, T>, RM extends RestModel> void updateElement(InternalActionContext ac, String uuid,
			TxHandler<RootVertex<T>> handler) {
		operateNoTx(ac, () -> {
			Database db = MeshInternal.get().database();
			RootVertex<T> root = handler.call();
			T element = root.loadObjectByUuid(ac, uuid, UPDATE_PERM);
			SearchQueue queue = MeshInternal.get().boot().meshRoot().getSearchQueue();

			Tuple<SearchQueueBatch, RM> tuple = db.tx(() -> {
				SearchQueueBatch batch = queue.createBatch();
				T updatedElement = element.update(ac, batch);
				RM model = updatedElement.transformToRestSync(ac, 0);
				return Tuple.tuple(batch, model);
			});
			// The updating transaction has succeeded. Now lets store it in the index
			return db.noTx(() -> {
				SearchQueueBatch batch = tuple.v1();
				batch.process().await();
				RM model = tuple.v2();
				return model;
			});
		}, model -> ac.send(model, OK));
	}

	/**
	 * Read the element with the given element by loading it from the specified root vertex.
	 * 
	 * @param ac
	 * @param uuid
	 *            Uuid of the element which should be loaded
	 * @param handler
	 *            Handler which provides the root vertex which should be used when loading the element
	 */
	public static <T extends MeshCoreVertex<RM, T>, RM extends RestModel> void readElement(InternalActionContext ac, String uuid,
			TxHandler<RootVertex<T>> handler) {
		operateNoTx(ac, () -> {
			RootVertex<T> root = handler.call();
			T element = root.loadObjectByUuid(ac, uuid, READ_PERM);
			String etag = element.getETag(ac);
			ac.setEtag(etag, true);
			if (ac.matches(etag, true)) {
				throw new NotModifiedException();
			} else {
				return element.transformToRestSync(ac, 0);
			}
		}, (model) -> ac.send(model, OK));

	}

	/**
	 * Read a list of elements of the given root vertex and respond with a list response.
	 * 
	 * @param ac
	 * @param handler
	 *            Handler which provides the root vertex which should be used when loading the element
	 */
	public static <T extends MeshCoreVertex<RM, T>, RM extends RestModel> void readElementList(InternalActionContext ac,
			TxHandler<RootVertex<T>> handler) {
		operateNoTx(ac, () -> {
			RootVertex<T> root = handler.call();

			PagingParameters pagingInfo = new PagingParameters(ac);
			PageImpl<? extends T> page = root.findAll(ac, pagingInfo);

			// Handle etag
			String etag = page.getETag(ac);
			ac.setEtag(etag, true);
			if (ac.matches(etag, true)) {
				throw new NotModifiedException();
			} else {
				return page.transformToRest(ac, 0).toBlocking().value();
			}
		}, (e) -> ac.send(e, OK));
	}

	/**
	 * Asynchronously execute the handler within a scope of a no tx transaction.
	 * 
	 * @param ac
	 * @param handler
	 *            Handler which will be executed within a worker thread
	 * @param action
	 *            Action which will be invoked once the handler has finished
	 */
	public static <RM extends RestModel> void operateNoTx(InternalActionContext ac, TxHandler<RM> handler, Action1<RM> action) {
		Database db = MeshInternal.get().database();
		operate(ac, () -> {
			return db.noTx(handler);
		}, action);
	}

	/**
	 * Asynchronously execute the handler
	 * 
	 * @param ac
	 * @param handler
	 * @param action
	 */
	private static <RM extends RestModel> void operate(InternalActionContext ac, TxHandler<RM> handler, Action1<RM> action) {
		Mesh.vertx().executeBlocking(bc -> {
			try {
				bc.complete(handler.call());
			} catch (Exception e) {
				bc.fail(e);
			}
		}, (AsyncResult<RM> rh) -> {
			if (rh.failed()) {
				ac.fail(rh.cause());
			} else {
				action.call(rh.result());
			}
		});
	}

	/**
	 * Asynchronously execute the trxHandler within the scope of a non transaction.
	 * 
	 * @param trxHandler
	 * @return
	 */
	public static <T> Single<T> operateNoTx(TxHandler<Single<T>> trxHandler) {
		// Create an exception which we can use to enhance error information in case of timeout or other tranaction errors
		final AtomicReference<Exception> reference = new AtomicReference<Exception>(null);
		try {
			throw new Exception("Transaction timeout exception");
		} catch (Exception e1) {
			reference.set(e1);
		}

		return Single.create(sub -> {
			Database db = MeshInternal.get().database();
			Mesh.vertx().executeBlocking(bc -> {
				try (NoTx noTx = db.noTx()) {
					Single<T> result = trxHandler.call();
					if (result == null) {
						bc.complete();
					} else {
						try {
							T ele = result.toBlocking().toFuture().get(20, TimeUnit.SECONDS);
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

}
