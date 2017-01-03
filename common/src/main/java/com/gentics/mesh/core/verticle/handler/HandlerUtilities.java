package com.gentics.mesh.core.verticle.handler;

import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.IndexableElement;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.error.NotModifiedException;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.graphdb.spi.TxHandler;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.util.ResultInfo;

import io.vertx.core.AsyncResult;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.functions.Action1;

@Singleton
public class HandlerUtilities {

	private static final Logger log = LoggerFactory.getLogger(HandlerUtilities.class);

	private Database database;
	private SearchQueue searchQueue;

	@Inject
	public HandlerUtilities(Database database, SearchQueue searchQueue) {
		this.searchQueue = searchQueue;
		this.database = database;
	}

	/**
	 * Create an object using the given aggregation node and respond with a transformed object.
	 * 
	 * @param ac
	 * @param handler
	 */
	public <T extends MeshCoreVertex<RM, T>, RM extends RestModel> void createElement(InternalActionContext ac, TxHandler<RootVertex<T>> handler) {
		operateNoTx(ac, () -> {
			ResultInfo info = database.tx(() -> {
				RootVertex<T> root = handler.call();
				SearchQueueBatch batch = searchQueue.createBatch();
				T created = root.create(ac, batch);
				RM model = created.transformToRestSync(ac, 0);
				String path = created.getAPIPath(ac);
				ResultInfo resultInfo = new ResultInfo(model, batch);
				resultInfo.setProperty("path", path);
				return resultInfo;
			});

			RestModel model = info.getModel();
			String path = info.getProperty("path");
			SearchQueueBatch batch = info.getBatch();
			ac.setLocation(path);
			batch.processSync();
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
	public <T extends MeshCoreVertex<RM, T>, RM extends RestModel> void deleteElement(InternalActionContext ac, TxHandler<RootVertex<T>> handler,
			String uuid) {
		operateNoTx(ac, () -> {
			RootVertex<T> root = handler.call();
			T element = root.loadObjectByUuid(ac, uuid, DELETE_PERM);
			SearchQueueBatch batch = searchQueue.createBatch();
			String elementUuid = element.getUuid();
			database.tx(() -> {
				// Check whether the element is indexable. Indexable elements must also be purged from the search index.
				if (element instanceof IndexableElement) {
					element.delete(batch);
					return element;
				} else {
					throw error(INTERNAL_SERVER_ERROR, "Could not determine object name");
				}
			});
			log.info("Deleted element {" + elementUuid + "}");
			return database.noTx(() -> {
				batch.processSync();
				return (RM) null;
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
	public <T extends MeshCoreVertex<RM, T>, RM extends RestModel> void updateElement(InternalActionContext ac, String uuid,
			TxHandler<RootVertex<T>> handler) {
		operateNoTx(ac, () -> {
			RootVertex<T> root = handler.call();

			// 1. Load the element from the root element using the given uuid
			T element = root.loadObjectByUuid(ac, uuid, UPDATE_PERM);

			// 2. Create a batch before starting the update to prevent creation of duplicate batches due to trx retry actions
			SearchQueueBatch batch = searchQueue.createBatch();
			RM model = database.tx(() -> {
				T updatedElement = element.update(ac, batch);
				return updatedElement.transformToRestSync(ac, 0);
			});

			// 3. The updating transaction has succeeded. Now lets store it in the index
			return database.noTx(() -> {
				batch.processSync();
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
	public <T extends MeshCoreVertex<RM, T>, RM extends RestModel> void readElement(InternalActionContext ac, String uuid,
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
	public <T extends MeshCoreVertex<RM, T>, RM extends RestModel> void readElementList(InternalActionContext ac, TxHandler<RootVertex<T>> handler) {
		operateNoTx(ac, () -> {
			RootVertex<T> root = handler.call();

			PagingParameters pagingInfo = ac.getPagingParameters();
			Page<? extends T> page = root.findAll(ac, pagingInfo);

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
	public <RM extends RestModel> void operateNoTx(InternalActionContext ac, TxHandler<RM> handler, Action1<RM> action) {
		operate(ac, () -> {
			return database.noTx(handler);
		}, action);
	}

	/**
	 * Asynchronously execute the handler
	 * 
	 * @param ac
	 * @param handler
	 * @param action
	 */
	private <RM extends RestModel> void operate(InternalActionContext ac, TxHandler<RM> handler, Action1<RM> action) {
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
