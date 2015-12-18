package com.gentics.mesh.core.verticle.handler;

import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException.error;
import static com.gentics.mesh.json.JsonUtil.toJson;
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

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;

/**
 * Abstract class for CRUD REST handlers.
 */
public abstract class AbstractCrudHandler<T extends MeshCoreVertex<? extends RestModel, T>> extends AbstractHandler {

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
	 */
	abstract public void handleDelete(InternalActionContext ac);

	/**
	 * Handle read requests that target a single object.
	 * 
	 * @param ac
	 */
	public void handleRead(InternalActionContext ac) {
		readElement(ac, "uuid", () -> getRootVertex(ac));
	}

	/**
	 * Handle update requests.
	 * 
	 * @param ac
	 */
	public void handleUpdate(InternalActionContext ac) {
		updateElement(ac, "uuid", () -> getRootVertex(ac));
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
	protected void createElement(InternalActionContext ac, TrxHandler2<RootVertex<?>> handler) {
		db.asyncNoTrx(noTrx -> {
			RootVertex<?> root;
			try {
				root = handler.call();
				root.create(ac, rh -> {
					if (ac.failOnError(rh)) {
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
		} , rh -> {
			ac.errorHandler().handle(rh);
		});
	}

	protected <E extends MeshCoreVertex<?, E>> void deleteElement(InternalActionContext ac, TrxHandler2<RootVertex<E>> handler,
			String uuidParameterName, String responseMessage) {

		db.asyncNoTrx(noTrx -> {
			RootVertex<?> root = handler.call();
			root.loadObject(ac, uuidParameterName, DELETE_PERM, rh -> {
				if (ac.failOnError(rh)) {
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
							txDeleted.result().v2().process(ac, brh -> {
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
				if (ac.failOnError(rh)) {
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
				if (ac.failOnError(lh)) {
					lh.result().transformToRest(ac, th -> {
						if (ac.failOnError(th)) {
							ac.respond(th.result(), OK);
						}
					});
				}
			});
		} , rh -> {
			ac.errorHandler().handle(rh);
		});

	}

	// <E extends MeshCoreVertex<TR, E>, TR extends RestModel>
	protected void readElementList(InternalActionContext ac, TrxHandler2<RootVertex<T>> handler) {
		db.asyncNoTrx(noTrx -> {
			RootVertex<T> root = handler.call();
			root.loadObjects(ac, rh -> {
				if (ac.failOnError(rh)) {
					ac.send(toJson(rh.result()), OK);
				}
			});
		} , rh -> {
			ac.errorHandler().handle(rh);
		});
	}

}
