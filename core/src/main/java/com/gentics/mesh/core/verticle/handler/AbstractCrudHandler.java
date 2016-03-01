package com.gentics.mesh.core.verticle.handler;

import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.common.GenericMessageResponse.message;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import org.elasticsearch.common.collect.Tuple;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.IndexableElement;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.NamedElement;
import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.graphdb.spi.TrxHandler;
import com.gentics.mesh.query.impl.PagingParameter;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import rx.Observable;

/**
 * Abstract class for CRUD REST handlers.
 */
public abstract class AbstractCrudHandler<T extends MeshCoreVertex<RM, T>, RM extends RestModel> extends AbstractHandler {

	public static final String TAGFAMILY_ELEMENT_CONTEXT_DATA_KEY = "rootElement";

	/**
	 * Return the main root vertex that is used to handle CRUD for the elements that are used in combination with this handler.
	 * 
	 * @param ac
	 * @return
	 */
	abstract public RootVertex<T> getRootVertex(InternalActionContext ac);

	/**
	 * Handle create requests.
	 * 
	 * @param ac
	 */
	public void handleCreate(InternalActionContext ac) {
		createElement(ac, () -> getRootVertex(ac));
	}

	/**
	 * Handle delete requests.
	 * 
	 * @param ac
	 * @param uuid
	 *            Uuid of the element which should be deleted
	 */
	abstract public void handleDelete(InternalActionContext ac, String uuid);

	/**
	 * Handle read requests that target a single object.
	 * 
	 * @param ac
	 * @param uuid
	 *            Uuid of the element which should be read
	 */
	public void handleRead(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");
		readElement(ac, uuid, () -> getRootVertex(ac));
	}

	/**
	 * Handle update requests.
	 * 
	 * @param ac
	 * @param uuid
	 *            Uuid of the element which should be updated
	 */
	public void handleUpdate(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");
		updateElement(ac, uuid, () -> getRootVertex(ac));
	}

	/**
	 * Handle read list requests.
	 * 
	 * @param ac
	 */
	public void handleReadList(InternalActionContext ac) {
		readElementList(ac, () -> getRootVertex(ac));
	}

	/**
	 * Create an object using the given aggregation node and respond with a transformed object.
	 * 
	 * @param ac
	 * @param handler
	 */

	protected void createElement(InternalActionContext ac, TrxHandler<RootVertex<?>> handler) {
		db.asyncNoTrxExperimental(() -> {
			RootVertex<?> root = handler.call();
			return root.create(ac).flatMap(created -> {
				// Transform the vertex using a fresh transaction in order to start with a clean cache
				return db.noTrx(() -> {
					// created.reload();
					return created.transformToRest(ac);
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
	protected void deleteElement(InternalActionContext ac, TrxHandler<RootVertex<T>> handler, String uuid, String responseMessage) {
		validateParameter(uuid, "uuid");

		db.asyncNoTrxExperimental(() -> {
			RootVertex<T> root = handler.call();
			Observable<T> obs = root.loadObjectByUuid(ac, uuid, DELETE_PERM);
			return obs.flatMap(element -> {

				return db.noTrx(() -> {

					Tuple<String, SearchQueueBatch> tuple = db.trx(() -> {

						String elementUuid = element.getUuid();
						if (element instanceof IndexableElement) {
							SearchQueueBatch batch = ((IndexableElement) element).addIndexBatch(SearchQueueEntryAction.DELETE_ACTION);
							String name = null;
							if (element instanceof NamedElement) {
								name = ((NamedElement) element).getName();
							}
							final String objectName = name;
							String id = objectName != null ? elementUuid + "/" + objectName : elementUuid;
							element.delete();
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
	 * 
	 * @param ac
	 * @param uuid
	 *            Uuid of the element which should be updated
	 * @param handler
	 *            Handler which provides the root vertex which should be used when loading the element
	 * 
	 */
	protected void updateElement(InternalActionContext ac, String uuid, TrxHandler<RootVertex<T>> handler) {
		db.asyncNoTrxExperimental(() -> {
			RootVertex<T> root = handler.call();
			return root.loadObjectByUuid(ac, uuid, UPDATE_PERM).flatMap(element -> {
				return element.update(ac).flatMap(updatedElement -> {
					// Transform the vertex using a fresh transaction in order to start with a clean cache
					return db.noTrx(() -> {
						updatedElement.reload();
						return updatedElement.transformToRest(ac);
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
	protected void readElement(InternalActionContext ac, String uuid, TrxHandler<RootVertex<?>> handler) {
		db.asyncNoTrxExperimental(() -> {
			RootVertex<?> root = handler.call();
			return root.loadObjectByUuid(ac, uuid, READ_PERM).flatMap(node -> {
				return node.transformToRest(ac);
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
	protected void readElementList(InternalActionContext ac, TrxHandler<RootVertex<T>> handler) {
		db.asyncNoTrxExperimental(() -> {
			RootVertex<T> root = handler.call();

			PagingParameter pagingInfo = ac.getPagingParameter();
			MeshAuthUser requestUser = ac.getUser();

			PageImpl<? extends T> page = root.findAll(requestUser, pagingInfo);
			return page.transformToRest(ac);

		}).subscribe(model -> ac.respond(model, OK), ac::fail);
	}

	/**
	 * Create a route handler which will load the element for the given uuid. The handler will only try to load the root element if a uuid was specified.
	 * Otherwise {@link RoutingContext#next()} will be invoked directly.
	 * 
	 * @param i18nNotFoundMessage
	 *            I18n error message that will be returned when no element could be found
	 */
	public Handler<RoutingContext> getUuidHandler(String i18nNotFoundMessage) {
		Handler<RoutingContext> handler = rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("param0");
			// Only try to load the root element when a uuid string was specified
			if (!isEmpty(uuid)) {
				boolean result = db.noTrx(() -> {
					T foundElement = getRootVertex(ac).findByUuid(uuid).toBlocking().single();
					if (foundElement == null) {
						throw error(NOT_FOUND, i18nNotFoundMessage, uuid);
					} else {
						ac.data().put(TAGFAMILY_ELEMENT_CONTEXT_DATA_KEY, foundElement);
					}
					return true;
				});
				if (!result) {
					return;
				}
			}
			rc.next();
		};
		return handler;
	}

}
