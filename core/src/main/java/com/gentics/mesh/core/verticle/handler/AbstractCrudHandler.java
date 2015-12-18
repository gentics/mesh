package com.gentics.mesh.core.verticle.handler;

import static com.gentics.mesh.json.JsonUtil.toJson;
import static com.gentics.mesh.util.VerticleHelper.hasSucceeded;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.graphdb.spi.TrxHandler2;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.util.VerticleHelper;

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

	protected void createElement(InternalActionContext ac, TrxHandler2<RootVertex<?>> handler) {

	}

	protected <T extends MeshCoreVertex<?, T>> void deleteElement(InternalActionContext ac, TrxHandler2<RootVertex<T>> handler,
			String uuidParameterName, String responseMessage) {

	}

	protected <T extends MeshCoreVertex<?, T>> void updateElement(InternalActionContext ac, String uuidParameterName,
			TrxHandler2<RootVertex<T>> handler) {

	}

	protected void readElement(InternalActionContext ac, String uuidParameterName, TrxHandler2<RootVertex<?>> handler) {
	}

	protected <T extends MeshCoreVertex<TR, T>, TR extends RestModel> void readElementList(InternalActionContext ac,
			TrxHandler2<RootVertex<T>> handler) {

		db.asyncNoTrx(noTrx -> {
			RootVertex<T> root = handler.call();
			VerticleHelper.loadObjects(ac,root, rh -> {
				if (hasSucceeded(ac, rh)) {
					ac.send(toJson(rh.result()), OK);
				}
			});
		} , rh -> {
			ac.errorHandler().handle(rh);
		});

	}
	//
	// /**
	// * Create an object using the given aggregation node and respond with a transformed object.
	// *
	// * @param ac
	// * @param root
	// * Aggregation node that should be used to create the object.
	// */
	// protected <T extends MeshCoreVertex<?,T>> void createObject(InternalActionContext ac, RootVertex<T> root) {
	// Database db = MeshSpringConfiguration.getInstance().database();
	//
	// root.create(ac, rh -> {
	// if (hasSucceeded(ac, rh)) {
	// MeshCoreVertex<?> vertex = rh.result();
	// // Transform the vertex using a fresh transaction in order to start with a clean cache
	// db.noTrx(noTx -> {
	// vertex.reload();
	// transformAndRespond(ac, vertex, CREATED);
	// });
	// }
	// });
	// }
	//
	// /**
	// * Update the object which is identified by the uuid parameter name and the aggregation root node.
	// *
	// * @param ac
	// * @param uuidParameterName
	// * @param root
	// */
	// protected <T extends MeshCoreVertex<?, ?>> void updateObject(InternalActionContext ac, String uuidParameterName, RootVertex<T> root) {
	// Database db = MeshSpringConfiguration.getInstance().database();
	// root.loadObject(ac, uuidParameterName, UPDATE_PERM, rh -> {
	// if (hasSucceeded(ac, rh)) {
	// MeshCoreVertex<?, ?> vertex = rh.result();
	// vertex.update(ac).subscribe(done -> {
	// // Transform the vertex using a fresh transaction in order to start with a clean cache
	// db.noTrx(noTx -> {
	// vertex.reload();
	// transformAndRespond(ac, vertex, OK);
	// });
	// } , error -> {
	// ac.fail(error);
	// });
	// }
	// });
	// }
	//
	// /**
	// * Delete the object that is identified by the uuid and the aggregation root node.
	// *
	// * @param ac
	// * @param uuidParameterName
	// * @param i18nMessageKey
	// * I18n message key that will be used to create a specific generic message response.
	// * @param root
	// */
	// protected <T extends MeshCoreVertex<? extends RestModel>> void deleteObject(InternalActionContext ac, String uuidParameterName,
	// String i18nMessageKey, RootVertex<T> root) {
	// Database db = MeshSpringConfiguration.getInstance().database();
	//
	// root.loadObject(ac, uuidParameterName, DELETE_PERM, rh -> {
	// if (hasSucceeded(ac, rh)) {
	// MeshCoreVertex<?> vertex = rh.result();
	// String uuid = vertex.getUuid();
	// String name = null;
	// if (vertex instanceof NamedElement) {
	// name = ((NamedElement) vertex).getName();
	// }
	// final String objectName = name;
	// db.trx(txDelete -> {
	// if (vertex instanceof IndexableElement) {
	// SearchQueueBatch batch = ((IndexableElement) vertex).addIndexBatch(SearchQueueEntryAction.DELETE_ACTION);
	// vertex.delete();
	// txDelete.complete(batch);
	// } else {
	// txDelete.fail(error(INTERNAL_SERVER_ERROR, "Could not determine object name"));
	// }
	// } , (AsyncResult<SearchQueueBatch> txDeleted) -> {
	// if (txDeleted.failed()) {
	// ac.errorHandler().handle(Future.failedFuture(txDeleted.cause()));
	// } else {
	// String id = objectName != null ? uuid + "/" + objectName : uuid;
	// VerticleHelper.processOrFail2(ac, txDeleted.result(), brh -> {
	// ac.sendMessage(OK, i18nMessageKey, id);
	// });
	// }
	// });
	// }
	// });
	// }

}
