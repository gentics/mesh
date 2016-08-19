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

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.IndexableElement;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.NamedElement;
import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.error.NotModifiedException;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.graphdb.spi.TxHandler;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.util.UUIDUtil;

import rx.Single;

public final class HandlerUtilities {

	/**
	 * Create an object using the given aggregation node and respond with a transformed object.
	 * 
	 * @param ac
	 * @param handler
	 */
	public static <T extends MeshCoreVertex<RM, T>, RM extends RestModel> void createElement(InternalActionContext ac,
			TxHandler<RootVertex<?>> handler) {
		Database db = MeshSpringConfiguration.getInstance().database();
		db.asyncNoTx(() -> {
			RootVertex<?> root = handler.call();
			return root.create(ac).flatMap(created -> {
				// Transform the vertex using a fresh transaction in order to start with a clean cache
				return db.noTx(() -> {
					// created.reload();
					ac.setLocation(created.getAPIPath(ac));
					return created.transformToRest(ac, 0);
				});
			});
		}).subscribe(model -> ac.send(model, CREATED), ac::fail);
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

		Database db = MeshSpringConfiguration.getInstance().database();

		db.asyncNoTx(() -> {
			RootVertex<T> root = handler.call();
			return root.loadObjectByUuid(ac, uuid, DELETE_PERM).flatMap(element -> {
				return db.noTx(() -> {
					Tuple<String, SearchQueueBatch> tuple = db.tx(() -> {
						String elementUuid = element.getUuid();
						if (element instanceof IndexableElement) {
							SearchQueue queue = BootstrapInitializer.getBoot().meshRoot().getSearchQueue();
							SearchQueueBatch batch = queue.createBatch(UUIDUtil.randomUUID());
							String name = null;
							if (element instanceof NamedElement) {
								name = ((NamedElement) element).getName();
							}
							final String objectName = name;
							String id = objectName != null ? elementUuid + "/" + objectName : elementUuid;
							element.delete(batch);
							return Tuple.tuple(id, batch);
						} else {
							throw error(INTERNAL_SERVER_ERROR, "Could not determine object name");
						}
					});
					//TODO add log output
//					String id = tuple.v1();
					SearchQueueBatch batch = tuple.v2();
					return batch.process().andThen(Single.just(null));
				});
			});
		}).subscribe(model -> ac.send(NO_CONTENT), ac::fail);

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
		Database db = MeshSpringConfiguration.getInstance().database();
		db.asyncNoTx(() -> {
			RootVertex<T> root = handler.call();
			return root.loadObjectByUuid(ac, uuid, UPDATE_PERM).flatMap(element -> {
				return element.update(ac).flatMap(updatedElement -> {
					// Transform the vertex using a fresh transaction in order to start with a clean cache
					return db.noTx(() -> {
						updatedElement.reload();
						return updatedElement.transformToRest(ac, 0);
					});
				});
			});
		}).subscribe(model -> ac.send(model, OK), ac::fail);

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
			TxHandler<RootVertex<?>> handler) {
		Database db = MeshSpringConfiguration.getInstance().database();
		db.asyncNoTx(() -> {
			RootVertex<?> root = handler.call();
			return root.loadObjectByUuid(ac, uuid, READ_PERM).flatMap(element -> {
				String etag = element.getETag(ac);
				ac.setEtag(etag, true);
				if (ac.matches(etag, true)) {
					return Single.error(new NotModifiedException());
				} else {
					return element.transformToRest(ac, 0);
				}
			});
		}).subscribe(model -> ac.send(model, OK), ac::fail);
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
		Database db = MeshSpringConfiguration.getInstance().database();
		db.asyncNoTx(() -> {
			RootVertex<T> root = handler.call();

			PagingParameters pagingInfo = new PagingParameters(ac);
			PageImpl<? extends T> page = root.findAll(ac, pagingInfo);

			// Handle etag
			String etag = page.getETag(ac);
			ac.setEtag(etag, true);
			if (ac.matches(etag, true)) {
				return Single.error(new NotModifiedException());
			} else {
				return page.transformToRest(ac, 0);
			}

		}).subscribe(model -> ac.send(model, OK), ac::fail);
	}

}
