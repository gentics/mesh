package com.gentics.mesh.core.verticle.handler;

import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException.error;
import static com.gentics.mesh.util.VerticleHelper.hasSucceeded;
import static com.gentics.mesh.util.VerticleHelper.transformAndRespond;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import com.gentics.mesh.core.data.GenericVertex;
import com.gentics.mesh.core.data.IndexedVertex;
import com.gentics.mesh.core.data.NamedVertex;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
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
	 * @param root
	 *            Aggregation node that should be used to create the object.
	 */
	protected <T extends GenericVertex<?>> void createObject(InternalActionContext ac, RootVertex<T> root) {
		Database db = MeshSpringConfiguration.getInstance().database();

		root.create(ac, rh -> {
			if (hasSucceeded(ac, rh)) {
				GenericVertex<?> vertex = rh.result();
				// Transform the vertex using a fresh transaction in order to start with a clean cache
				db.noTrx(noTx -> {
					vertex.reload();
					transformAndRespond(ac, vertex, CREATED);
				});
			}
		});
	}

	/**
	 * Update the object which is identified by the uuid parameter name and the aggregation root node.
	 * 
	 * @param ac
	 * @param uuidParameterName
	 * @param root
	 */
	protected <T extends GenericVertex<?>> void updateObject(InternalActionContext ac, String uuidParameterName, RootVertex<T> root) {
		Database db = MeshSpringConfiguration.getInstance().database();
		root.loadObject(ac, uuidParameterName, UPDATE_PERM, rh -> {
			if (hasSucceeded(ac, rh)) {
				GenericVertex<?> vertex = rh.result();
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
	}

	/**
	 * Delete the object that is identified by the uuid and the aggregation root node.
	 * 
	 * @param ac
	 * @param uuidParameterName
	 * @param i18nMessageKey
	 *            I18n message key that will be used to create a specific generic message response.
	 * @param root
	 */
	protected <T extends GenericVertex<? extends RestModel>> void deleteObject(InternalActionContext ac, String uuidParameterName,
			String i18nMessageKey, RootVertex<T> root) {
		Database db = MeshSpringConfiguration.getInstance().database();

		root.loadObject(ac, uuidParameterName, DELETE_PERM, rh -> {
			if (hasSucceeded(ac, rh)) {
				GenericVertex<?> vertex = rh.result();
				String uuid = vertex.getUuid();
				String name = null;
				if (vertex instanceof NamedVertex) {
					name = ((NamedVertex) vertex).getName();
				}
				final String objectName = name;
				db.trx(txDelete -> {
					if (vertex instanceof IndexedVertex) {
						SearchQueueBatch batch = ((IndexedVertex) vertex).addIndexBatch(SearchQueueEntryAction.DELETE_ACTION);
						vertex.delete();
						txDelete.complete(batch);
					} else {
						txDelete.fail(error(INTERNAL_SERVER_ERROR, "Could not determine object name"));
					}
				} , (AsyncResult<SearchQueueBatch> txDeleted) -> {
					if (txDeleted.failed()) {
						ac.errorHandler().handle(Future.failedFuture(txDeleted.cause()));
					} else {
						String id = objectName != null ? uuid + "/" + objectName : uuid;
						VerticleHelper.processOrFail2(ac, txDeleted.result(), brh -> {
							ac.sendMessage(OK, i18nMessageKey, id);
						});
					}
				});
			}
		});
	}

}
