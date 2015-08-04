package com.gentics.mesh.util;

import static com.gentics.mesh.core.data.relationship.Permission.DELETE_PERM;
import static com.gentics.mesh.core.data.search.SearchQueue.SEARCH_QUEUE_ENTRY_ADDRESS;
import static com.gentics.mesh.json.JsonUtil.toJson;
import static com.gentics.mesh.util.RoutingContextHelper.getPagingInfo;
import static com.gentics.mesh.util.RoutingContextHelper.getUser;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.cli.Mesh;
import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.GenericVertex;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.NamedNode;
import com.gentics.mesh.core.data.relationship.Permission;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.data.service.I18NService;
import com.gentics.mesh.core.rest.common.AbstractListResponse;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.error.EntityNotFoundException;
import com.gentics.mesh.error.InvalidPermissionException;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.syncleus.ferma.FramedThreadedTransactionalGraph;

public class VerticleHelper {

	public static <T extends GenericVertex<TR>, TR extends RestModel, RL extends AbstractListResponse<TR>> void loadObjects(RoutingContext rc,
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

	public static <T extends GenericVertex<? extends RestModel>> void delete(RoutingContext rc, String uuidParameterName, String i18nMessageKey,
			RootVertex<T> root) {
		I18NService i18n = I18NService.getI18n();

		loadObject(
				rc,
				uuidParameterName,
				DELETE_PERM,
				root,
				rh -> {
					if (hasSucceeded(rc, rh)) {
						GenericVertex<?> vertex = rh.result();
						String uuid = vertex.getUuid();
						String name = null;
						if (vertex instanceof NamedNode) {
							name = ((NamedNode) vertex).getName();
						}
						FramedThreadedTransactionalGraph fg = MeshSpringConfiguration.getMeshSpringConfiguration()
								.getFramedThreadedTransactionalGraph();
						try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
							vertex.delete();
							BootstrapInitializer.getBoot().meshRoot().getSearchQueue()
									.put(vertex.getUuid(), vertex.getType(), SearchQueueEntryAction.DELETE_ACTION);
							Mesh.vertx().eventBus().send(SEARCH_QUEUE_ENTRY_ADDRESS, null);
							tx.success();
						}
						String id = name != null ? uuid + "/" + name : uuid;
						responde(rc, toJson(new GenericMessageResponse(i18n.get(rc, i18nMessageKey, id))));
					}
				});
	}

	public static <T extends GenericVertex<? extends RestModel>> void loadTransformAndResponde(RoutingContext rc, String uuidParameterName,
			Permission permission, RootVertex<T> root) {
		loadAndTransform(rc, uuidParameterName, permission, root, rh -> {
			if (hasSucceeded(rc, rh)) {
				responde(rc, toJson(rh.result()));
			}
		});
	}

	public static <T extends GenericVertex<TR>, TR extends RestModel, RL extends AbstractListResponse<TR>> void transformAndResponde(
			RoutingContext rc, Page<T> page, RL listResponse) {
		transformPage(rc, page, th -> {
			if (hasSucceeded(rc, th)) {
				responde(rc, toJson(th.result()));
			}
		}, listResponse);
	}

	public static <T extends GenericVertex<TR>, TR extends RestModel, RL extends AbstractListResponse<TR>> void transformPage(RoutingContext rc,
			Page<T> page, Handler<AsyncResult<AbstractListResponse<TR>>> handler, RL listResponse) {
		for (T node : page) {
			node.transformToRest(rc, rh -> {
				listResponse.getData().add(rh.result());
			});
		}
		RestModelPagingHelper.setPaging(listResponse, page);
		handler.handle(Future.succeededFuture(listResponse));
	}

	public static <T extends GenericVertex<TR>, TR extends RestModel> void loadTransformAndResponde(RoutingContext rc, RootVertex<T> root,
			AbstractListResponse<TR> listResponse) {
		loadObjects(rc, root, rh -> {
			if (hasSucceeded(rc, rh)) {
				responde(rc, toJson(rh.result()));
			}
		}, listResponse);
	}

	public static <T extends RestModel> void transformAndResponde(RoutingContext rc, GenericVertex<T> node) {
		node.transformToRest(rc, th -> {
			if (hasSucceeded(rc, th)) {
				responde(rc, toJson(th.result()));
			}
		});
	}

	public static void responde(RoutingContext rc, String body) {
		rc.response().putHeader("content-type", AbstractWebVerticle.APPLICATION_JSON);
		rc.response().setStatusCode(200).end(body);
	}

	public static <T extends GenericVertex<? extends RestModel>> void loadAndTransform(RoutingContext rc, String uuidParameterName,
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

	public static <T extends GenericVertex<?>> void loadObject(RoutingContext rc, String uuidParameterName, Permission perm, RootVertex<T> root,
			Handler<AsyncResult<T>> handler) {

		I18NService i18n = I18NService.getI18n();
		String uuid = rc.request().params().get(uuidParameterName);
		if (StringUtils.isEmpty(uuid)) {
			handler.handle(Future.failedFuture(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "error_request_parameter_missing",
					uuidParameterName))));
		} else {
			loadObjectByUuid(rc, uuid, perm, root, handler);
		}
	}

	public static <T extends GenericVertex<?>> void loadObjectByUuid(RoutingContext rc, String uuid, Permission perm, RootVertex<T> root,

	Handler<AsyncResult<T>> handler) {
		if (root == null) {
			// TODO i18n
			handler.handle(Future.failedFuture("Could not find root node."));
		} else {
			I18NService i18n = I18NService.getI18n();
			try (BlueprintTransaction tx = new BlueprintTransaction(MeshSpringConfiguration.getMeshSpringConfiguration()
					.getFramedThreadedTransactionalGraph())) {
				root.findByUuid(
						uuid,
						rh -> {
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
										handler.handle(Future.failedFuture(new InvalidPermissionException(i18n.get(rc, "error_missing_perm",
												node.getUuid()))));
									}
								}
							}
						});
			}
		}

	}

	public static boolean hasSucceeded(RoutingContext rc, AsyncResult<?> result) {
		if (result.failed()) {
			rc.fail(result.cause());
			return false;
		}
		return true;
	}
}
