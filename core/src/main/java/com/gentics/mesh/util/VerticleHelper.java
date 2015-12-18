package com.gentics.mesh.util;

import static com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException.failedFuture;
import static com.gentics.mesh.json.JsonUtil.toJson;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.handler.InternalActionContext;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * 
 * @deprecated Please don't add stuff to this class since it should be removed anyway.
 *
 */
public class VerticleHelper {

	private static final Logger log = LoggerFactory.getLogger(VerticleHelper.class);

	// TODO merge with prev method
	public static <T extends MeshCoreVertex<TR, T>, TR extends RestModel, RL extends ListResponse<TR>> void processOrFail2(ActionContext ac,
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
	public static <T extends MeshCoreVertex<TR, T>, TR extends RestModel, RL extends ListResponse<TR>> void processOrFail(InternalActionContext ac,
			SearchQueueBatch batch, Handler<AsyncResult<T>> handler, T element) {

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

	public static <T extends MeshCoreVertex<? extends RestModel, T>> void loadTransformAndRespond(InternalActionContext ac, String uuidParameterName,
			GraphPermission permission, RootVertex<T> root, HttpResponseStatus status) {
		loadAndTransform(ac, uuidParameterName, permission, root, rh -> {
			if (ac.failOnError(rh)) {
				ac.send(toJson(rh.result()), status);
			}
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
	public static <T extends MeshCoreVertex<? extends RestModel, T>> void loadAndTransform(InternalActionContext ac, String uuidParameterName,
			GraphPermission permission, RootVertex<T> root, Handler<AsyncResult<RestModel>> handler) {
		root.loadObject(ac, uuidParameterName, permission, rh -> {
			if (ac.failOnError(rh)) {
				// TODO handle nested exceptions differently
				try {
					rh.result().transformToRest(ac, th -> {
						if (ac.failOnError(th)) {
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
	public static <T extends RestModel> void transformAndRespond(InternalActionContext ac, MeshCoreVertex<?, ?> vertex,
			HttpResponseStatus statusCode) {
		vertex.transformToRest(ac, th -> {
			if (ac.failOnError(th)) {
				ac.send(toJson(th.result()), statusCode);
			}
		});
	}

}
