package com.gentics.mesh.core.verticle.handler;

import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

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
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.graphdb.spi.TxHandler;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.tinkerpop.gremlin.Tokens.T;

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
		operate(ac, () -> {
			Database db = MeshInternal.get().database();
			return db.noTx(() -> {
				RootVertex<T> root = handler.call();
				SearchQueue queue = MeshInternal.get().boot().meshRoot().getSearchQueue();
				Tuple<T, SearchQueueBatch> tuple = db.tx(() -> {
					SearchQueueBatch batch = queue.createBatch();
					return Tuple.tuple(root.create(ac, batch), batch);
				});
				return db.noTx(() -> {
					T created = tuple.v1();
					SearchQueueBatch batch = tuple.v2();
					ac.setLocation(created.getAPIPath(ac));
					 batch.process().toObservable().lastOrDefault(null);
					return created.transformToRestSync(ac, 0);
				});
			});
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
		operate(ac, () -> {
			Database db = MeshInternal.get().database();
			return db.noTx(() -> {
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
			});
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
		operate(ac, () -> {
			Database db = MeshInternal.get().database();
			return db.noTx(() -> {
				RootVertex<T> root = handler.call();
				T element = root.loadObjectByUuid(ac, uuid, UPDATE_PERM);
				SearchQueue queue = MeshInternal.get().boot().meshRoot().getSearchQueue();
				SearchQueueBatch batch = queue.createBatch();
				T updatedElement = element.update(ac, batch);

				// Transform the vertex using a fresh transaction in order to start with a clean cache
				return db.noTx(() -> {
					updatedElement.reload();
					return batch.process().andThen(updatedElement.transformToRest(ac, 0)).toBlocking().value();
				});
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
		operate(ac, () -> {
			Database db = MeshInternal.get().database();
			return db.noTx(() -> {
				RootVertex<T> root = handler.call();
				T element = root.loadObjectByUuid(ac, uuid, READ_PERM);
				String etag = element.getETag(ac);
				ac.setEtag(etag, true);
				if (ac.matches(etag, true)) {
					throw new NotModifiedException();
				} else {
					return element.transformToRestSync(ac, 0);
				}
			});
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
		operate(ac, () -> {
			Database db = MeshInternal.get().database();
			return db.noTx(() -> {
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
			});
		}, (e) -> ac.send(e, OK));
	}

	/**
	 * Asynchronously execute the handler
	 * 
	 * @param ac
	 * @param handler
	 * @param action
	 */
	public static <RM extends RestModel> void operate(InternalActionContext ac, TxHandler<RM> handler, Action1<RM> action) {
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

}
