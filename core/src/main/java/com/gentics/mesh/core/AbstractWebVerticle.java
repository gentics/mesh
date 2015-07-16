package com.gentics.mesh.core;

import static com.gentics.mesh.core.data.relationship.Permission.DELETE_PERM;
import static com.gentics.mesh.json.JsonUtil.toJson;
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
import com.gentics.mesh.core.data.GenericVertex;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.NamedNode;
import com.gentics.mesh.core.data.relationship.Permission;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.common.AbstractListResponse;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.error.EntityNotFoundException;
import com.gentics.mesh.error.InvalidPermissionException;
import com.gentics.mesh.etc.config.MeshConfigurationException;
import com.gentics.mesh.util.BlueprintTransaction;
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

	protected <T extends GenericVertex<TR>, TR extends RestModel, RL extends AbstractListResponse<TR>> void loadObjects(RoutingContext rc,
			RootVertex<T> root, Handler<AsyncResult<AbstractListResponse<TR>>> handler, RL listResponse) {
		PagingInfo pagingInfo = getPagingInfo(rc);
		MeshAuthUser requestUser = getUser(rc);
		try {
			Page<? extends T> page = root.findAll(requestUser, pagingInfo);
			for (T node : page) {
				node.transformToRest(rc, rh -> {
					if (hasSucceeded(rc, rh)) {
						listResponse.getData().add(rh.result());
					}
					// TODO handle async issue
					});
			}
			RestModelPagingHelper.setPaging(listResponse, page);
			handler.handle(Future.succeededFuture(listResponse));
		} catch (InvalidArgumentException e) {
			handler.handle(Future.failedFuture(e));
		}
	}

	protected <T extends GenericVertex<? extends RestModel>> void delete(RoutingContext rc, String uuidParameterName, String i18nMessageKey,
			RootVertex<T> root) {
		loadObject(rc, "uuid", DELETE_PERM, root, rh -> {
			if (hasSucceeded(rc, rh)) {
				GenericVertex<?> node = rh.result();
				String uuid = node.getUuid();
				String name = null;
				if (node instanceof NamedNode) {
					name = ((NamedNode) node).getName();
				}
				try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
					node.delete();
					tx.success();
				}
				String id = name != null ? uuid + "/" + name : uuid;
				responde(rc, toJson(new GenericMessageResponse(i18n.get(rc, i18nMessageKey, id))));
			}
		});
	}

	protected <T extends GenericVertex<? extends RestModel>> void loadTransformAndResponde(RoutingContext rc, String uuidParameterName,
			Permission permission, RootVertex<T> root) {
		loadAndTransform(rc, uuidParameterName, permission, root, rh -> {
			if (hasSucceeded(rc, rh)) {
				responde(rc, toJson(rh.result()));
			}
		});
	}

	protected <T extends GenericVertex<TR>, TR extends RestModel, RL extends AbstractListResponse<TR>> void transformAndResponde(
			RoutingContext rc, Page<T> page, RL listResponse) {
		transformPage(rc, page, th -> {
			if (hasSucceeded(rc, th)) {
				responde(rc, toJson(th.result()));
			}
		}, listResponse);
	}

	protected <T extends GenericVertex<TR>, TR extends RestModel, RL extends AbstractListResponse<TR>> void transformPage(RoutingContext rc,
			Page<T> page, Handler<AsyncResult<AbstractListResponse<TR>>> handler, RL listResponse) {
		for (T node : page) {
			node.transformToRest(rc, rh -> {
				listResponse.getData().add(rh.result());
			});
		}
		RestModelPagingHelper.setPaging(listResponse, page);
		handler.handle(Future.succeededFuture(listResponse));
	}

	protected <T extends GenericVertex<TR>, TR extends RestModel> void loadTransformAndResponde(RoutingContext rc, RootVertex<T> root,
			AbstractListResponse<TR> listResponse) {
		loadObjects(rc, root, rh -> {
			if (hasSucceeded(rc, rh)) {
				responde(rc, toJson(rh.result()));
			}
		}, listResponse);
	}

	protected <T extends RestModel> void transformAndResponde(RoutingContext rc, GenericVertex<T> node) {
		node.transformToRest(rc, th -> {
			if (hasSucceeded(rc, th)) {
				responde(rc, toJson(th.result()));
			}
		});
	}

	protected void responde(RoutingContext rc, String body) {
		rc.response().putHeader("content-type", APPLICATION_JSON);
		rc.response().setStatusCode(200).end(body);
	}

	protected <T extends GenericVertex<? extends RestModel>> void loadAndTransform(RoutingContext rc, String uuidParameterName,
			Permission permission, RootVertex<T> root, Handler<AsyncResult<RestModel>> handler) {
		loadObject(rc, uuidParameterName, permission, root, rh -> {
			if (hasSucceeded(rc, rh)) {
				// TODO handle nested exceptions differently
				try {
					rh.result().transformToRest(rc, th -> {
						if (hasSucceeded(rc, th)) {
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

	protected <T extends GenericVertex<?>> void loadObject(RoutingContext rc, String uuidParameterName, Permission perm, RootVertex<T> root,
			Handler<AsyncResult<T>> handler) {
		String uuid = rc.request().params().get(uuidParameterName);
		if (StringUtils.isEmpty(uuid)) {
			handler.handle(Future.failedFuture(new HttpStatusCodeErrorException(400, i18n.get(rc, "error_request_parameter_missing",
					uuidParameterName))));
		} else {
			loadObjectByUuid(rc, uuid, perm, root, handler);
		}
	}

	protected <T extends GenericVertex<?>> void loadObjectByUuid(RoutingContext rc, String uuid, Permission perm, RootVertex<T> root,

	Handler<AsyncResult<T>> handler) {
		if (root == null) {
			// TODO i18n
			handler.handle(Future.failedFuture("Could not find root node."));
		} else {
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

	}

	protected boolean hasSucceeded(RoutingContext rc, AsyncResult<?> result) {
		if (result.failed()) {
			rc.fail(result.cause());
			return false;
		}
		return true;
	}

}
