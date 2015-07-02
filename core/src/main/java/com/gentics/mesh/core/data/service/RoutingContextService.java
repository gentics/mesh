package com.gentics.mesh.core.data.service;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.ASSIGNED_TO_PROJECT;
import static com.gentics.mesh.util.RoutingContextHelper.getUser;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.relationship.Permission;
import com.gentics.mesh.error.EntityNotFoundException;
import com.gentics.mesh.error.HttpStatusCodeErrorException;
import com.gentics.mesh.error.InvalidPermissionException;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.util.BlueprintTransaction;
import com.gentics.mesh.util.RoutingContextHelper;
import com.syncleus.ferma.FramedThreadedTransactionalGraph;

@Component
public class RoutingContextService {

	private static final Logger log = LoggerFactory.getLogger(RoutingContextService.class);

	@Autowired
	private FramedThreadedTransactionalGraph fg;

	@Autowired
	private MeshSpringConfiguration configuration;

	@Autowired
	private I18NService i18n;

	public String getProjectName(RoutingContext rc) {
		return rc.get(RouterStorage.PROJECT_CONTEXT_KEY);
	}

	public <T extends MeshVertex> void loadObjectByUuid(RoutingContext rc, String uuid, Permission permType, Class<? extends T> classOfT,
			Handler<AsyncResult<T>> resultHandler) {
		loadObjectByUuid(rc, uuid, null, permType, classOfT, resultHandler, null);
	}

	public <T extends MeshVertex> void loadObjectByUuid(RoutingContext rc, String uuid, Permission permType, Class<? extends T> classOfT,
			Handler<AsyncResult<T>> resultHandler, Handler<AsyncResult<T>> transactionCompletedHandler) {
		loadObjectByUuid(rc, uuid, null, permType, classOfT, resultHandler, transactionCompletedHandler);
	}

	public <T extends MeshVertex> T findByUUID(String projectName, String uuid, Class<? extends T> classOfT) {
		return fg.v().has("uuid", uuid).mark().out(ASSIGNED_TO_PROJECT).has("name", projectName).back().nextOrDefault(classOfT, null);
	}

	public <T extends MeshVertex> T findByUUID(String uuid, Class<T> classOfT) {
		return fg.v().has("uuid", uuid).has(classOfT).nextOrDefault(classOfT, null);
	}

	public <T extends MeshVertex> void loadObjectByUuid(RoutingContext rc, String uuid, String projectName, Permission permType, Class<? extends T> classOfT,
			Handler<AsyncResult<T>> resultHandler, Handler<AsyncResult<T>> transactionCompletedHandler) {
		if (StringUtils.isEmpty(uuid)) {
			// TODO i18n, add info about uuid source?
			throw new HttpStatusCodeErrorException(400, i18n.get(rc, "error_uuid_must_be_specified"));
		}

		configuration.vertx().executeBlocking((Future<T> fut) -> {
			// TODO add generic loading and framing of objects
				T node = null;
				try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
					if (projectName != null) {
						node = findByUUID(projectName, uuid, classOfT);
					} else {
						node = findByUUID(uuid, classOfT);
					}
					tx.success();
				}
				if (node == null) {
					fut.fail(new EntityNotFoundException(i18n.get(rc, "object_not_found_for_uuid", uuid)));
					return;
				}
				final T foundNode = node;
				MeshAuthUser requestUser = RoutingContextHelper.getUser(rc);
				requestUser.isAuthorised(node, permType, handler -> {
					if (!handler.result()) {
						fut.fail(new InvalidPermissionException(i18n.get(rc, "error_missing_perm", foundNode.getUuid())));
						return;
					} else {
						fut.complete(foundNode);
						return;
					}
				});
			}, res -> {
				if (res.failed()) {
					rc.fail(res.cause());
				} else {
					try {
						if (resultHandler != null) {
							resultHandler.handle(res);
						}
						if (transactionCompletedHandler != null) {
							AsyncResult<T> transactionCompletedFuture = Future.succeededFuture(res.result());
							transactionCompletedHandler.handle(transactionCompletedFuture);
						}
					} catch (Exception e) {
						rc.fail(e);
					}
				}
			});

	}

	public <T extends MeshVertex> void loadObject(RoutingContext rc, String uuidParamName, String projectName, Permission permType,
			Class<? extends T> classOfT, Handler<AsyncResult<T>> resultHandler, Handler<AsyncResult<T>> transactionCompleteHandler) {
		String uuid = rc.request().params().get(uuidParamName);
		if (StringUtils.isEmpty(uuid)) {
			rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "error_request_parameter_missing", uuidParamName)));
			return;
		}

		loadObjectByUuid(rc, uuid, projectName, permType, classOfT, resultHandler, transactionCompleteHandler);

	}

	public <T extends MeshVertex> void loadObject(RoutingContext rc, String uuidParamName, Permission permType, Class<? extends T> classOfT,
			Handler<AsyncResult<T>> resultHandler) {
		loadObject(rc, uuidParamName, permType, classOfT, resultHandler, null);
	}

	public <T extends MeshVertex> void loadObject(RoutingContext rc, String uuidParamName, String projectName, Permission permType,
			Class<? extends T> classOfT, Handler<AsyncResult<T>> resultHandler) {
		loadObject(rc, uuidParamName, projectName, permType, classOfT, resultHandler, null);
	}

	public <T extends MeshVertex> void loadObject(RoutingContext rc, String uuidParamName, Permission permType, Class<? extends T> classOfT,
			Handler<AsyncResult<T>> resultHandler, Handler<AsyncResult<T>> transactionCompleteHandler) {
		loadObject(rc, uuidParamName, null, permType, classOfT, resultHandler, transactionCompleteHandler);
	}


	/**
	 * Check the permission and throw an invalid permission exception when no matching permission could be found.
	 */
	//TODO move this to MeshAuthUser class
	public void hasPermission(RoutingContext rc, MeshVertex node, Permission type, Handler<AsyncResult<Boolean>> resultHandler,
			Handler<AsyncResult<Boolean>> transactionCompletedHandler) throws InvalidPermissionException {
		MeshAuthUser requestUser = getUser(rc);
		requestUser.isAuthorised(node, type, handler -> {
			if (!handler.result()) {
				rc.fail(new InvalidPermissionException(i18n.get(rc, "error_missing_perm", node.getUuid())));
				AsyncResult<Boolean> transactionCompletedFuture = Future.succeededFuture(true);
				transactionCompletedHandler.handle(transactionCompletedFuture);
			} else {
				resultHandler.handle(Future.succeededFuture(handler.result()));
				if (transactionCompletedHandler != null) {
					AsyncResult<Boolean> transactionCompletedFuture = Future.succeededFuture(true);
					transactionCompletedHandler.handle(transactionCompletedFuture);
				}
			}
		});
	}

}
