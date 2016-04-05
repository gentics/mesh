package com.gentics.mesh.core.verticle.handler;

import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.common.GenericMessageResponse.message;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import org.elasticsearch.common.collect.Tuple;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.IndexableElement;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.NamedElement;
import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.graphdb.spi.TrxHandler;
import com.gentics.mesh.query.impl.PagingParameter;
import com.gentics.mesh.util.UUIDUtil;

import rx.Observable;

public final class HandlerUtilities {

	/**
	 * Create an object using the given aggregation node and respond with a transformed object.
	 * 
	 * @param ac
	 * @param handler
	 */
	public static <T extends MeshCoreVertex<RM, T>, RM extends RestModel> void createElement(InternalActionContext ac,
			TrxHandler<RootVertex<?>> handler) {
		Database db = MeshSpringConfiguration.getInstance().database();
		db.asyncNoTrxExperimental(() -> {
			RootVertex<?> root = handler.call();
			return root.create(ac).flatMap(created -> {
				// Transform the vertex using a fresh transaction in order to start with a clean cache
				return db.noTrx(() -> {
					// created.reload();
					return created.transformToRest(ac, 0);
				});
			});
		}).subscribe(model -> ac.respond(model, CREATED), ac::fail);
	}

	/**
	 * Delete the specified element.
	 * 
	 * @param ac
	 * @param handler
	 *            Handler which provides the root vertex which will be used to load the element
	 * @param uuid
	 *            Uuid of the element which should be deleted
	 * @param responseMessage
	 *            Response message to be returned on success
	 */
	public static <T extends MeshCoreVertex<RM, T>, RM extends RestModel> void deleteElement(InternalActionContext ac,
			TrxHandler<RootVertex<T>> handler, String uuid, String responseMessage) {

		Database db = MeshSpringConfiguration.getInstance().database();
		db.asyncNoTrxExperimental(() -> {
			RootVertex<T> root = handler.call();
			Observable<T> obs = root.loadObjectByUuid(ac, uuid, DELETE_PERM);
			return obs.flatMap(element -> {

				return db.noTrx(() -> {

					Tuple<String, SearchQueueBatch> tuple = db.trx(() -> {

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

					String id = tuple.v1();
					SearchQueueBatch batch = tuple.v2();
					return batch.process().map(done -> {
						return message(ac, responseMessage, id);
					});
				});
			});
		}).subscribe(model -> ac.respond(model, OK), ac::fail);

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
			TrxHandler<RootVertex<T>> handler) {
		Database db = MeshSpringConfiguration.getInstance().database();
		db.asyncNoTrxExperimental(() -> {
			RootVertex<T> root = handler.call();
			return root.loadObjectByUuid(ac, uuid, UPDATE_PERM).flatMap(element -> {
				return element.update(ac).flatMap(updatedElement -> {
					// Transform the vertex using a fresh transaction in order to start with a clean cache
					return db.noTrx(() -> {
						updatedElement.reload();
						return updatedElement.transformToRest(ac, 0);
					});
				});
			});
		}).subscribe(model -> ac.respond(model, OK), ac::fail);

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
			TrxHandler<RootVertex<?>> handler) {
		Database db = MeshSpringConfiguration.getInstance().database();
		db.asyncNoTrxExperimental(() -> {
			RootVertex<?> root = handler.call();
			return root.loadObjectByUuid(ac, uuid, READ_PERM).flatMap(node -> {
				return node.transformToRest(ac, 0);
			});
		}).subscribe(model -> ac.respond(model, OK), ac::fail);
	}

	/**
	 * Read a list of elements of the given root vertex and respond with a list response.
	 * 
	 * @param ac
	 * @param handler
	 *            Handler which provides the root vertex which should be used when loading the element
	 */
	public static <T extends MeshCoreVertex<RM, T>, RM extends RestModel> void readElementList(InternalActionContext ac,
			TrxHandler<RootVertex<T>> handler) {
		Database db = MeshSpringConfiguration.getInstance().database();
		db.asyncNoTrxExperimental(() -> {
			RootVertex<T> root = handler.call();

			PagingParameter pagingInfo = ac.getPagingParameter();
			MeshAuthUser requestUser = ac.getUser();

			PageImpl<? extends T> page = root.findAll(requestUser, pagingInfo);
			return page.transformToRest(ac, 0);

		}).subscribe(model -> ac.respond(model, OK), ac::fail);
	}

}
