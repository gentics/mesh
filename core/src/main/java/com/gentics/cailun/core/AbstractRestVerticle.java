package com.gentics.cailun.core;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.apex.Route;
import io.vertx.ext.apex.Router;
import io.vertx.ext.apex.RoutingContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.cailun.core.data.model.auth.CaiLunPermission;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.model.generic.AbstractPersistable;
import com.gentics.cailun.error.EntityNotFoundException;
import com.gentics.cailun.error.HttpStatusCodeErrorException;
import com.gentics.cailun.error.InvalidPermissionException;
import com.gentics.cailun.etc.config.CaiLunConfigurationException;

public abstract class AbstractRestVerticle extends AbstractSpringVerticle {

	private static final Logger log = LoggerFactory.getLogger(AbstractRestVerticle.class);

	public static final String APPLICATION_JSON = ContentType.APPLICATION_JSON.getMimeType();

	protected Router localRouter = null;
	protected String basePath;
	protected HttpServer server;

	protected AbstractRestVerticle(String basePath) {
		this.basePath = basePath;
	}

	@Override
	public void start() throws Exception {
		this.localRouter = setupLocalRouter();
		if (localRouter == null) {
			throw new CaiLunConfigurationException("The local router was not setup correctly. Startup failed.");
		}

		log.info("Starting http server..");
		server = vertx.createHttpServer(new HttpServerOptions().setPort(config().getInteger("port")));
		server.requestHandler(routerStorage.getRootRouter()::accept);
		server.listen();
		log.info("Started http server.. Port: " + config().getInteger("port"));
		registerEndPoints();

	}

	public abstract void registerEndPoints() throws Exception;

	public abstract Router setupLocalRouter();

	@Override
	public void stop() throws Exception {
		localRouter.clear();
	}

	public Router getRouter() {
		return localRouter;
	}

	public HttpServer getServer() {
		return server;
	}

	/**
	 * Wrapper for getRouter().route(path)
	 * 
	 * @return
	 */
	protected Route route(String path) {
		Route route = localRouter.route(path);
		return route;
	}

	/**
	 * Wrapper for getRouter().route()
	 * 
	 * @return
	 */
	protected Route route() {
		Route route = localRouter.route();
		return route;
	}

	public <T extends AbstractPersistable> void loadObjectByUuid(RoutingContext rc, String uuid, PermissionType permType,
			Handler<AsyncResult<T>> resultHandler) {
		loadObjectByUuid(rc, uuid, null, permType, resultHandler, null);
	}

	public <T extends AbstractPersistable> void loadObjectByUuid(RoutingContext rc, String uuid, PermissionType permType,
			Handler<AsyncResult<T>> resultHandler, Handler<AsyncResult<T>> transactionCompletedHandler) {
		loadObjectByUuid(rc, uuid, null, permType, resultHandler, transactionCompletedHandler);
	}

	public <T extends AbstractPersistable> void loadObjectByUuid(RoutingContext rc, String uuid, String projectName, PermissionType permType,
			Handler<AsyncResult<T>> resultHandler, Handler<AsyncResult<T>> transactionCompletedHandler) {
		if (StringUtils.isEmpty(uuid)) {
			// TODO i18n, add info about uuid source?
			throw new HttpStatusCodeErrorException(400, i18n.get(rc, "error_uuid_must_be_specified"));
		}

		vertx.executeBlocking((Future<T> fut) -> {
			T node = null;
			if (projectName != null) {
				node = (T) genericNodeService.findByUUID(projectName, uuid);
			} else {
				node = (T) genericNodeService.findByUUID(uuid);
			}
			if (node == null) {
				fut.fail(new EntityNotFoundException(i18n.get(rc, "object_not_found_for_uuid", uuid)));
				return;
			}
			final T foundNode = node;
			rc.session().hasPermission(new CaiLunPermission(node, permType).toString(), handler -> {
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
						try (Transaction tx = graphDb.beginTx()) {
							resultHandler.handle(res);
							tx.success();
						}
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

	public <T extends AbstractPersistable> void loadObject(RoutingContext rc, String uuidParamName, String projectName, PermissionType permType,
			Handler<AsyncResult<T>> resultHandler, Handler<AsyncResult<T>> transactionCompleteHandler) {
		String uuid = rc.request().params().get(uuidParamName);
		if (StringUtils.isEmpty(uuid)) {
			rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "error_request_parameter_missing", uuidParamName)));
			return;
		}

		loadObjectByUuid(rc, uuid, projectName, permType, resultHandler, transactionCompleteHandler);

	}

	public <T extends AbstractPersistable> void loadObject(RoutingContext rc, String uuidParamName, PermissionType permType,
			Handler<AsyncResult<T>> resultHandler) {
		loadObject(rc, uuidParamName, permType, resultHandler, null);
	}

	public <T extends AbstractPersistable> void loadObject(RoutingContext rc, String uuidParamName,  String projectName, PermissionType permType,
			Handler<AsyncResult<T>> resultHandler) { 
		loadObject(rc, uuidParamName, projectName, permType, resultHandler, null);
	}

	public <T extends AbstractPersistable> void loadObject(RoutingContext rc, String uuidParamName, PermissionType permType,
			Handler<AsyncResult<T>> resultHandler, Handler<AsyncResult<T>> transactionCompleteHandler) {

		loadObject(rc, uuidParamName, null, permType, resultHandler, transactionCompleteHandler);

	}

	protected void hasPermission(RoutingContext rc, AbstractPersistable node, PermissionType type, Handler<AsyncResult<Boolean>> resultHandler) {
		hasPermission(rc, node, type, resultHandler, null);
	}

	/**
	 * Check the permission and throw an invalid permission exception when no matching permission could be found.
	 *
	 * @param rc
	 * @param node
	 * @param type
	 * @return
	 */
	protected void hasPermission(RoutingContext rc, AbstractPersistable node, PermissionType type, Handler<AsyncResult<Boolean>> resultHandler,
			Handler<AsyncResult<Boolean>> transactionCompletedHandler) throws InvalidPermissionException {
		rc.session().hasPermission(new CaiLunPermission(node, type).toString(), handler -> {
			if (!handler.result()) {
				rc.fail(new InvalidPermissionException(i18n.get(rc, "error_missing_perm", node.getUuid())));
				AsyncResult<Boolean> transactionCompletedFuture = Future.succeededFuture(true);
				transactionCompletedHandler.handle(transactionCompletedFuture);
			} else {
				try (Transaction tx = graphDb.beginTx()) {
					resultHandler.handle(Future.succeededFuture(handler.result()));
					tx.success();
				}
				if (transactionCompletedHandler != null) {
					AsyncResult<Boolean> transactionCompletedFuture = Future.succeededFuture(true);
					transactionCompletedHandler.handle(transactionCompletedFuture);
				}
			}
		});
	}

}
