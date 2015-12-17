package com.gentics.mesh.util;

import static com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException.failedFuture;
import static com.gentics.mesh.json.JsonUtil.toJson;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.GenericVertex;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.common.AbstractListResponse;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.query.impl.PagingParameter;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import rx.Observable;

/**
 * 
 * @deprecated Please don't add stuff to this class since it should be removed anyway.
 *
 */
public class VerticleHelper {

	private static final Logger log = LoggerFactory.getLogger(VerticleHelper.class);

	/**
	 * Load the objects from the root vertex using the paging parameter from the action context and send a JSON transformed response.
	 * 
	 * @param ac
	 * @param root
	 * @param listResponse
	 */
	public static <T extends GenericVertex<TR>, TR extends RestModel> void loadTransformAndRespond(InternalActionContext ac, RootVertex<T> root,
			AbstractListResponse<TR> listResponse, HttpResponseStatus statusCode) {
		loadObjects(ac, root, rh -> {
			if (hasSucceeded(ac, rh)) {
				ac.send(toJson(rh.result()), statusCode);
			}
		} , listResponse);
	}



	// TODO merge with prev method
	public static <T extends GenericVertex<TR>, TR extends RestModel, RL extends AbstractListResponse<TR>> void processOrFail2(ActionContext ac,
			SearchQueueBatch batch, Handler<AsyncResult<Void>> handler) {

		processBatch(ac, batch, rh -> {
			if (rh.failed()) {
				handler.handle(Future.failedFuture(rh.cause()));
			} else {
				handler.handle(Future.succeededFuture());
			}
		});
	}

	/**
	 * Process the given batch and call the handler when the batch was processed.
	 * 
	 * @param ac
	 * @param batch
	 *            Batch to be processed
	 * @param handler
	 *            Result handler that will be invoked on completion or error
	 */
	public static <T> void processBatch(ActionContext ac, SearchQueueBatch batch, Handler<AsyncResult<Future<T>>> handler) {
		Database db = MeshSpringConfiguration.getInstance().database();
		BootstrapInitializer boot = BootstrapInitializer.getBoot();

		if (batch == null) {
			// TODO i18n
			log.error("Batch was not set. Can't process search index batch.");
			handler.handle(failedFuture(INTERNAL_SERVER_ERROR, "indexing_not_possible"));
		}

		// 1. Remove the batch from the queue
		db.trx(tc -> {
			SearchQueue searchQueue = boot.meshRoot().getSearchQueue();
			searchQueue.reload();
			searchQueue.remove(batch);
			tc.complete(searchQueue);
		} , sqrh -> {
			if (sqrh.failed()) {
				handler.handle(Future.failedFuture(sqrh.cause()));
			} else {
				// 2. Process the batch
				db.noTrx(txProcess -> {
					batch.process(rh -> {
						// 3. Add the batch back to the queue when an error occurs
						if (rh.failed()) {
							db.trx(txAddBack -> {
								SearchQueue searchQueue = boot.meshRoot().getSearchQueue();
								batch.reload();
								log.error("Error while processing batch {" + batch.getBatchId() + "}. Adding batch back to queue.", rh.cause());
								searchQueue.add(batch);
								txAddBack.complete(batch);
							} , txAddedBack -> {
								if (txAddedBack.failed()) {
									log.error("Failed to add batch {" + batch.getBatchId() + "} batck to search queue.", txAddedBack.cause());
								}
							});
							// Inform the caller that processing failed
							handler.handle(failedFuture(BAD_REQUEST, "search_index_batch_process_failed", rh.cause()));
						} else {
							// Inform the caller that processing completed
							handler.handle(Future.succeededFuture());
						}
					});
				});
			}
		});
	}

	/**
	 * 
	 * @param ac
	 * @param batch
	 * @param handler
	 * @param element
	 */
	public static <T extends GenericVertex<TR>, TR extends RestModel, RL extends AbstractListResponse<TR>> void processOrFail(
			InternalActionContext ac, SearchQueueBatch batch, Handler<AsyncResult<T>> handler, T element) {

		if (element == null) {
			// TODO log
			// TODO i18n
			ac.fail(BAD_REQUEST, "element creation failed");
			return;
		} else {
			processBatch(ac, batch, rh -> {
				if (rh.failed()) {
					handler.handle(Future.failedFuture(rh.cause()));
				} else {
					handler.handle(Future.succeededFuture(element));
				}
			});

		}
	}
	
	/**
	 * Asynchronously load the objects and populate the given list response.
	 * 
	 * @param ac
	 *            Action context that will be used to extract the paging parameters from
	 * @param root
	 *            Aggregation node that should be used to load the objects
	 * @param handler
	 *            Handler which will be invoked once all objects have been loaded and transformed and the list response is completed
	 * @param listResponse
	 */
	private static <T extends GenericVertex<TR>, TR extends RestModel, RL extends AbstractListResponse<TR>> void loadObjects(InternalActionContext ac,
			RootVertex<T> root, Handler<AsyncResult<AbstractListResponse<TR>>> handler, RL listResponse) {

		// TODO use reflection to create the empty list response

		PagingParameter pagingInfo = ac.getPagingParameter();
		MeshAuthUser requestUser = ac.getUser();
		try {
			Page<? extends T> page = root.findAll(requestUser, pagingInfo);
			List<ObservableFuture<TR>> transformedElements = new ArrayList<>();
			for (T node : page) {
				ObservableFuture<TR> obs = RxHelper.observableFuture();
				transformedElements.add(obs);
				node.transformToRest(ac, obs.toHandler());
			}
			RxUtil.concatList(transformedElements).collect(() -> {
				return listResponse;
			} , (list, restElement) -> {
				list.getData().add(restElement);
			}).subscribe(list -> {
				page.setPaging(listResponse);
				handler.handle(Future.succeededFuture(listResponse));
			} , error -> {
				handler.handle(Future.failedFuture(error));
			});
		} catch (InvalidArgumentException e) {
			handler.handle(Future.failedFuture(e));
		}
	}


	public static <T extends GenericVertex<? extends RestModel>> void loadTransformAndRespond(InternalActionContext ac, String uuidParameterName,
			GraphPermission permission, RootVertex<T> root, HttpResponseStatus status) {
		loadAndTransform(ac, uuidParameterName, permission, root, rh -> {
			if (hasSucceeded(ac, rh)) {
				ac.send(toJson(rh.result()), status);
			}
		});
	}

	/**
	 * Transform the given page to a rest page and send it to the client.
	 * 
	 * @param ac
	 * @param page
	 * @param listResponse
	 * @param status
	 */
	public static <T extends GenericVertex<TR>, TR extends RestModel, RL extends AbstractListResponse<TR>> void transformAndRespond(
			InternalActionContext ac, Page<T> page, RL listResponse, HttpResponseStatus status) {
		transformPage(ac, page, th -> {
			if (hasSucceeded(ac, th)) {
				ac.send(toJson(th.result()), status);
			}
		} , listResponse);
	}

	/**
	 * Transform the page into a list response.
	 * 
	 * @param ac
	 * @param page
	 * @param handler
	 * @param listResponse
	 */
	public static <T extends GenericVertex<TR>, TR extends RestModel, RL extends AbstractListResponse<TR>> void transformPage(
			InternalActionContext ac, Page<T> page, Handler<AsyncResult<AbstractListResponse<TR>>> handler, RL listResponse) {
		List<ObservableFuture<TR>> futures = new ArrayList<>();

		for (T node : page) {
			ObservableFuture<TR> obs = RxHelper.observableFuture();
			futures.add(obs);
			node.transformToRest(ac, obs.toHandler());
		}

		// Wait for all async processes to complete
		Observable.merge(futures).collect(() -> {
			return listResponse.getData();
		} , (x, y) -> {
			x.add(y);
		}).subscribe(list -> {
			page.setPaging(listResponse);
			handler.handle(Future.succeededFuture(listResponse));
		} , error -> {
			handler.handle(Future.failedFuture(error));
		});
	}

	/**
	 * Load the object with the UUID which is taken from the routing context parameter and transform it into a rest model. Call the given handler when the
	 * object was loaded or when loading failed.
	 * 
	 * @param ac
	 * @param uuidParameterName
	 * @param permission
	 * @param root
	 * @param handler
	 */
	public static <T extends GenericVertex<? extends RestModel>> void loadAndTransform(InternalActionContext ac, String uuidParameterName,
			GraphPermission permission, RootVertex<T> root, Handler<AsyncResult<RestModel>> handler) {
		root.loadObject(ac, uuidParameterName, permission, rh -> {
			if (hasSucceeded(ac, rh)) {
				// TODO handle nested exceptions differently
				try {
					rh.result().transformToRest(ac, th -> {
						if (hasSucceeded(ac, th)) {
							handler.handle(Future.succeededFuture(th.result()));
						} else {
							handler.handle(Future.failedFuture("Not authorized"));
						}
					});
				} catch (HttpStatusCodeErrorException e) {
					handler.handle(Future.failedFuture(e));
				}
			}
		});
	}

	/**
	 * Transform the given vertex to a rest model and send with a JSON document response.
	 * 
	 * @param ac
	 * @param vertex
	 * @param statusCode
	 */
	public static <T extends RestModel> void transformAndRespond(InternalActionContext ac, GenericVertex<T> vertex, HttpResponseStatus statusCode) {
		vertex.transformToRest(ac, th -> {
			if (hasSucceeded(ac, th)) {
				ac.send(toJson(th.result()), statusCode);
			}
		});
	}

	/**
	 * Create a generic message response and send it as a JSON document.
	 * 
	 * @param ac
	 * @param i18nKey
	 * @param parameters
	 */
	public static void respond(ActionContext ac, String i18nKey, HttpResponseStatus statusCode, String... parameters) {
		GenericMessageResponse msg = new GenericMessageResponse();
		msg.setMessage(ac.i18n(i18nKey, parameters));
		ac.send(toJson(msg), statusCode);
	}

	/**
	 * Check the result object and fail early when the result failed as well.
	 * 
	 * @param ac
	 * @param result
	 *            Result that will be checked
	 * @return false when the result failed, otherwise true
	 */
	public static boolean hasSucceeded(InternalActionContext ac, AsyncResult<?> result) {
		if (result.failed()) {
			ac.fail(result.cause());
			return false;
		}
		return true;
	}

}
