package com.gentics.mesh.core;

import static com.gentics.mesh.util.RoutingContextHelper.getPagingInfo;
import static com.gentics.mesh.util.RoutingContextHelper.getUser;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.data.GenericNode;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.relationship.Permission;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.common.AbstractListResponse;
import com.gentics.mesh.core.rest.common.AbstractRestModel;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.error.EntityNotFoundException;
import com.gentics.mesh.error.InvalidPermissionException;
import com.gentics.mesh.etc.config.MeshConfigurationException;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.RestModelPagingHelper;

public abstract class AbstractWebVerticle extends AbstractSpringVerticle {

	private static final Logger log = LoggerFactory.getLogger(AbstractWebVerticle.class);

	public static final String APPLICATION_JSON = ContentType.APPLICATION_JSON.getMimeType();

	protected Router localRouter = null;
	protected String basePath;
	protected HttpServer server;

	protected AbstractWebVerticle(String basePath) {
		this.basePath = basePath;
	}

	@Override
	public void start() throws Exception {
		this.localRouter = setupLocalRouter();
		if (localRouter == null) {
			throw new MeshConfigurationException("The local router was not setup correctly. Startup failed.");
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
	 */
	protected Route route(String path) {
		Route route = localRouter.route(path);
		return route;
	}

	/**
	 * Wrapper for getRouter().route()
	 */
	protected Route route() {
		Route route = localRouter.route();
		return route;
	}

	protected <T extends GenericNode<TR>, TR extends AbstractRestModel, RL extends AbstractListResponse<TR>> void loadObjects(
			RoutingContext rc, RootVertex<T,TR> root, Handler<AsyncResult<AbstractListResponse<TR>>> handler, RL listResponse) {
		PagingInfo pagingInfo = getPagingInfo(rc);
		MeshAuthUser requestUser = getUser(rc);
		try {
			Page<? extends T> page = root.findAll(requestUser, pagingInfo);
			for (T node : page) {
				node.transformToRest(requestUser, rh -> {
					listResponse.getData().add(rh.result());
				});
			}
			RestModelPagingHelper.setPaging(listResponse, page);
			handler.handle(Future.succeededFuture(listResponse));
		} catch (InvalidArgumentException e) {
			handler.handle(Future.failedFuture(e));
		}
	}

	protected <T extends GenericNode<TR>, TR extends AbstractRestModel, RL extends AbstractListResponse<TR>> void transformPage(RoutingContext rc,
			Page<T> page, Handler<AsyncResult<AbstractListResponse<TR>>> handler, RL listResponse) {
		MeshAuthUser requestUser = getUser(rc);
		for (T node : page) {
			node.transformToRest(requestUser, rh -> {
				listResponse.getData().add(rh.result());
			});
		}
		RestModelPagingHelper.setPaging(listResponse, page);
		handler.handle(Future.succeededFuture(listResponse));
	}

	protected <T extends GenericNode<? extends AbstractRestModel>> void loadAndTransform(RoutingContext rc, String uuidParameterName,
			Permission permission, RootVertex<T> root, Handler<AsyncResult<AbstractRestModel>> handler) {
		loadObject(rc, uuidParameterName, permission, root, rh -> {
			if (hasSucceeded(rc, rh)) {
				MeshAuthUser requestUser = getUser(rc);
				rh.result().transformToRest(requestUser, th -> {
					if (hasSucceeded(rc, th)) {
						handler.handle(Future.succeededFuture(th.result()));
					} else {
						handler.handle(Future.failedFuture(""));
					}
				});
			}
		});
	}

	protected <T extends GenericNode<?>> void loadObject(RoutingContext rc, String uuidParameterName, Permission perm, RootVertex<T> root,
			Handler<AsyncResult<T>> handler) {
		String uuid = rc.request().params().get(uuidParameterName);
		if (StringUtils.isEmpty(uuid)) {
			handler.handle(Future.failedFuture(new HttpStatusCodeErrorException(400, i18n.get(rc, "error_request_parameter_missing",
					uuidParameterName))));
		} else {
			loadObjectByUuid(rc, uuid, perm, root, handler);
		}
	}

	protected <T extends GenericNode<?>> void loadObjectByUuid(RoutingContext rc, String uuid, Permission perm, RootVertex<T> root,
			Handler<AsyncResult<T>> handler) {
		root.findByUuid(uuid, rh -> {
			if (rh.failed()) {
				handler.handle(Future.failedFuture(rh.cause()));
			} else {
				T node = rh.result();
				if (node == null) {
					handler.handle(Future.failedFuture(new EntityNotFoundException(i18n.get(rc, "object_not_found_for_uuid", uuid))));
				} else {
					MeshAuthUser requestUser = getUser(rc);
					if (requestUser.hasPermission(node, perm)) {
						handler.handle(Future.succeededFuture(node));
					} else {
						handler.handle(Future.failedFuture(new InvalidPermissionException(i18n.get(rc, "error_missing_perm", node.getUuid()))));
					}
				}
			}
		});

	}

	protected boolean hasSucceeded(RoutingContext rc, AsyncResult<?> result) {
		if (result.failed()) {
			rc.fail(result.cause());
			return false;
		}
		return true;
	}

}
