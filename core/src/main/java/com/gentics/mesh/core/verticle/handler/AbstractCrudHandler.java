package com.gentics.mesh.core.verticle.handler;

import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException.error;
import static com.gentics.mesh.json.JsonUtil.toJson;
import static com.gentics.mesh.util.VerticleHelper.hasSucceeded;
import static com.gentics.mesh.util.VerticleHelper.transformAndRespond;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import org.elasticsearch.common.collect.Tuple;

import com.gentics.mesh.core.data.IndexableElement;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.NamedElement;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.graphdb.spi.TrxHandler2;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.util.VerticleHelper;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;

/**
 * Abstract class for CRUD REST handlers.
 */
public abstract class AbstractCrudHandler extends AbstractHandler {

	/**
	 * Handle create requests.
	 * 
	 * @param ac
	 */
	abstract public void handleCreate(InternalActionContext ac);

	/**
	 * Handle delete requests.
	 * 
	 * @param ac
	 */
	abstract public void handleDelete(InternalActionContext ac);

	/**
	 * Handle update requests.
	 * 
	 * @param ac
	 */
	abstract public void handleUpdate(InternalActionContext ac);

	/**
	 * Handle read requests that target a single object.
	 * 
	 * @param ac
	 */
	abstract public void handleRead(InternalActionContext ac);

	/**
	 * Handle read list requests.
	 * 
	 * @param ac
	 */
	abstract public void handleReadList(InternalActionContext ac);

	/**
	 * Create an object using the given aggregation node and respond with a transformed object.
	 * 
	 * @param ac
	 * @param handler
	 */
	protected void createElement(InternalActionContext ac, TrxHandler2<RootVertex<?>> handler) {
		RootVertex<?> root;
		try {
			root = handler.call();
			root.create(ac, rh -> {
				if (hasSucceeded(ac, rh)) {
					MeshCoreVertex vertex = rh.result();
					// Transform the vertex using a fresh transaction in order to start with a clean cache
					db.noTrx(noTx -> {
						vertex.reload();
						transformAndRespond(ac, vertex, CREATED);
					});
				}
			});
		} catch (Exception e) {
			ac.fail(e);
		}
	}

	protected <T extends MeshCoreVertex<?, T>> void deleteElement(InternalActionContext ac, TrxHandler2<RootVertex<T>> handler,
			String uuidParameterName, String responseMessage) {

		db.asyncNoTrx(noTrx -> {
			RootVertex<?> root = handler.call();
			root.loadObject(ac, uuidParameterName, DELETE_PERM, rh -> {
				if (hasSucceeded(ac, rh)) {
					db.trx(txDelete -> {
						MeshCoreVertex<?, ?> vertex = rh.result();
						String uuid = vertex.getUuid();
						if (vertex instanceof IndexableElement) {
							SearchQueueBatch batch = ((IndexableElement) vertex).addIndexBatch(SearchQueueEntryAction.DELETE_ACTION);
							String name = null;
							if (vertex instanceof NamedElement) {
								name = ((NamedElement) vertex).getName();
							}
							final String objectName = name;
							String id = objectName != null ? uuid + "/" + objectName : uuid;
							vertex.delete();
							txDelete.complete(Tuple.tuple(id, batch));
						} else {
							txDelete.fail(error(INTERNAL_SERVER_ERROR, "Could not determine object name"));
						}

					} , (AsyncResult<Tuple<String, SearchQueueBatch>> txDeleted) -> {
						if (txDeleted.failed()) {
							ac.errorHandler().handle(Future.failedFuture(txDeleted.cause()));
						} else {
							VerticleHelper.processOrFail2(ac, txDeleted.result().v2(), brh -> {
								ac.sendMessage(OK, responseMessage, txDeleted.result().v1());
							});
						}
					});
				}
			});
		} , rh -> {
			ac.errorHandler().handle(rh);
		});

	}

	protected <T extends MeshCoreVertex<?, T>> void updateElement(InternalActionContext ac, String uuidParameterName,
			TrxHandler2<RootVertex<T>> handler) {
		db.asyncNoTrx(noTrx -> {
			RootVertex<?> root = handler.call();
			root.loadObject(ac, uuidParameterName, UPDATE_PERM, rh -> {
				if (hasSucceeded(ac, rh)) {
					MeshCoreVertex<?, ?> vertex = rh.result();
					vertex.update(ac).subscribe(done -> {
						// Transform the vertex using a fresh transaction in order to start with a clean cache
						db.noTrx(noTx -> {
							vertex.reload();
							transformAndRespond(ac, vertex, OK);
						});
					} , error -> {
						ac.fail(error);
					});
				}
			});

		} , rh -> {
			ac.errorHandler().handle(rh);
		});

	}

	protected void readElement(InternalActionContext ac, String uuidParameterName, TrxHandler2<RootVertex<?>> handler) {
		db.asyncNoTrx(noTrx -> {
			RootVertex<?> root = handler.call();
			root.loadObject(ac, uuidParameterName, READ_PERM, lh -> {
				if (hasSucceeded(ac, lh)) {
					lh.result().transformToRest(ac, th -> {
						if (hasSucceeded(ac, th)) {
							ac.respond(th.result(), OK);
						}
					});
				}
			});
		} , rh -> {
			ac.errorHandler().handle(rh);
		});

	}

	protected <T extends MeshCoreVertex<TR, T>, TR extends RestModel> void readElementList(InternalActionContext ac,
			TrxHandler2<RootVertex<T>> handler) {
		db.asyncNoTrx(noTrx -> {
			RootVertex<T> root = handler.call();
			VerticleHelper.loadObjects(ac, root, rh -> {
				if (hasSucceeded(ac, rh)) {
					ac.send(toJson(rh.result()), OK);
				}
			});
		} , rh -> {
			ac.errorHandler().handle(rh);
		});
	}

}
