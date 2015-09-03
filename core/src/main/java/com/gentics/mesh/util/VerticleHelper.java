package com.gentics.mesh.util;

import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.json.JsonUtil.toJson;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.GenericVertex;
import com.gentics.mesh.core.data.IndexedVertex;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.NamedVertex;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.common.AbstractListResponse;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.common.PagingMetaInfo;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.error.EntityNotFoundException;
import com.gentics.mesh.error.InvalidPermissionException;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.ActionContext;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class VerticleHelper {

	private static final Logger log = LoggerFactory.getLogger(VerticleHelper.class);

	public static <T extends GenericVertex<TR>, TR extends RestModel> void loadTransformAndResponde(ActionContext ac, RootVertex<T> root,
			AbstractListResponse<TR> listResponse) {
		loadObjects(ac, root, rh -> {
			if (hasSucceeded(ac, rh)) {
				ac.send(toJson(rh.result()));
			}
		} , listResponse);
	}

	public static void setPaging(AbstractListResponse<?> response, Page<?> page) {
		PagingMetaInfo info = response.getMetainfo();
		info.setCurrentPage(page.getNumber());
		info.setPageCount(page.getTotalPages());
		info.setPerPage(page.getPerPage());
		info.setTotalCount(page.getTotalElements());
	}

	// TODO merge with prev method
	public static <T extends GenericVertex<TR>, TR extends RestModel, RL extends AbstractListResponse<TR>> void processOrFail2(ActionContext ac,
			SearchQueueBatch batch, Handler<AsyncResult<Void>> handler) {
		Database db = MeshSpringConfiguration.getMeshSpringConfiguration().database();
		BootstrapInitializer boot = BootstrapInitializer.getBoot();

		// TODO i18n
		if (batch == null) {
			// TODO log
			ac.fail(BAD_REQUEST, "indexing_not_possible");
		} else {
			SearchQueue searchQueue;
			try (Trx txBatch = db.trx()) {
				searchQueue = boot.meshRoot().getSearchQueue();
				searchQueue.reload();
				searchQueue.remove(batch);
				txBatch.success();
			}

			try (Trx txBatch = MeshSpringConfiguration.getMeshSpringConfiguration().database().trx()) {
				batch.process(rh -> {
					if (rh.failed()) {
						try (Trx tx = db.trx()) {
							batch.reload();
							log.error("Error while processing batch {" + batch.getBatchId() + "}. Adding batch back to queue.", rh.cause());
							searchQueue.add(batch);
							tx.success();
						}
						ac.fail(BAD_REQUEST, "search_index_batch_process_failed", rh.cause());
					} else {
						handler.handle(Future.succeededFuture());
					}
				});
			}
		}
	}

	public static <T extends GenericVertex<TR>, TR extends RestModel, RL extends AbstractListResponse<TR>> void processOrFail(ActionContext ac,
			SearchQueueBatch batch, Handler<AsyncResult<T>> handler, T element) {

		Database db = MeshSpringConfiguration.getMeshSpringConfiguration().database();
		BootstrapInitializer boot = BootstrapInitializer.getBoot();

		// TODO i18n
		if (batch == null) {
			// TODO log
			ac.fail(BAD_REQUEST, "indexing_not_possible");
		} else if (element == null) {
			// TODO log
			ac.fail(BAD_REQUEST, "element creation failed");
		} else {
			SearchQueue searchQueue;
			try (Trx txBatch = db.trx()) {
				searchQueue = boot.meshRoot().getSearchQueue();
				searchQueue.reload();
				searchQueue.remove(batch);
				txBatch.success();
			}

			try (Trx txBatch = db.trx()) {
				batch.process(rh -> {
					if (rh.failed()) {
						log.error("Error while processing batch {" + batch.getBatchId() + "} for element {" + element.getUuid() + ":"
								+ element.getType() + "}.", rh.cause());
						try (Trx tx = db.trx()) {
							log.debug("Adding batch {" + batch.getBatchId() + "} back to queue");
							searchQueue.add(batch);
							tx.success();
						}
						ac.fail(BAD_REQUEST, "search_index_batch_process_failed", rh.cause());

					} else {
						handler.handle(Future.succeededFuture(element));
					}
				});
			}
		}
	}

	public static <T extends GenericVertex<TR>, TR extends RestModel, RL extends AbstractListResponse<TR>> void loadObjects(ActionContext ac,
			RootVertex<T> root, Handler<AsyncResult<AbstractListResponse<TR>>> handler, RL listResponse) {
		PagingInfo pagingInfo = ac.getPagingInfo();
		MeshAuthUser requestUser = ac.getUser();
		try {

			Page<? extends T> page = root.findAll(requestUser, pagingInfo);
			for (T node : page) {
				node.transformToRest(ac, rh -> {
					if (hasSucceeded(ac, rh)) {
						listResponse.getData().add(rh.result());
					}
					// TODO handle async issue
				});
			}
			setPaging(listResponse, page);
			handler.handle(Future.succeededFuture(listResponse));
		} catch (InvalidArgumentException e) {
			handler.handle(Future.failedFuture(e));
		}
	}

	public static <T extends GenericVertex<? extends RestModel>> void loadTransformAndResponde(ActionContext ac, String uuidParameterName,
			GraphPermission permission, RootVertex<T> root) {
		loadAndTransform(ac, uuidParameterName, permission, root, rh -> {
			if (hasSucceeded(ac, rh)) {
				ac.send(toJson(rh.result()));
			}
		});
	}

	public static <T extends GenericVertex<TR>, TR extends RestModel, RL extends AbstractListResponse<TR>> void transformAndResponde(ActionContext ac,
			Page<T> page, RL listResponse) {
		transformPage(ac, page, th -> {
			if (hasSucceeded(ac, th)) {
				ac.send(toJson(th.result()));
			}
		} , listResponse);
	}

	public static <T extends GenericVertex<TR>, TR extends RestModel, RL extends AbstractListResponse<TR>> void transformPage(ActionContext ac,
			Page<T> page, Handler<AsyncResult<AbstractListResponse<TR>>> handler, RL listResponse) {
		for (T node : page) {
			node.transformToRest(ac, rh -> {
				listResponse.getData().add(rh.result());
			});
		}
		setPaging(listResponse, page);
		handler.handle(Future.succeededFuture(listResponse));
	}

	public static <T extends GenericVertex<? extends RestModel>> void loadAndTransform(ActionContext ac, String uuidParameterName,
			GraphPermission permission, RootVertex<T> root, Handler<AsyncResult<RestModel>> handler) {
		loadObject(ac, uuidParameterName, permission, root, rh -> {
			if (hasSucceeded(ac, rh)) {
				// TODO handle nested exceptions differently
				try {
					rh.result().transformToRest(ac, th -> {
						if (hasSucceeded(ac, th)) {
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

	public static <T extends RestModel> void transformAndResponde(ActionContext ac, GenericVertex<T> node) {
		node.transformToRest(ac, th -> {
			if (hasSucceeded(ac, th)) {
				ac.send(toJson(th.result()));
			}
		});
	}

	public static void responde(ActionContext ac, String i18nKey, String... parameters) {
		GenericMessageResponse msg = new GenericMessageResponse();
		msg.setMessage(ac.i18n(i18nKey, parameters));
		ac.send(toJson(msg));
	}

	public static <T extends GenericVertex<?>> void createObject(ActionContext ac, RootVertex<T> root) {
		Database db = MeshSpringConfiguration.getMeshSpringConfiguration().database();
		root.create(ac, rh -> {
			if (hasSucceeded(ac, rh)) {
				GenericVertex<?> vertex = rh.result();
				// Transform the vertex using a fresh transaction in order to start with a clean cache
				try (Trx txi = db.trx()) {
					vertex.reload();
					transformAndResponde(ac, vertex);
				}
			}
		});
	}

	// public static <T extends GenericVertex<?>> void createObject(ActionContext ac, RootVertex<T> root) {
	// final int RETRY_COUNT = 15;
	//// Mesh.vertx().executeBlocking(bc -> {
	// AtomicBoolean hasFinished = new AtomicBoolean(false);
	// for (int i = 0; i < RETRY_COUNT && !hasFinished.get(); i++) {
	// try {
	// log.debug("Opening new transaction for try: {" + i + "}");
	// try (Trx tx = new Trx(MeshSpringConfiguration.getMeshSpringConfiguration().database())) {
	// if (log.isDebugEnabled()) {
	// log.debug("Invoking create on root vertex");
	// }
	// root.create(rc, rh -> {
	// if (rh.failed()) {
	// log.debug("Request for creation failed.", rh.cause());
	// } else {
	// GenericVertex<?> vertex = rh.result();
	// //triggerEvent(vertex.getUuid(), vertex.getType(), SearchQueueEntryAction.CREATE_ACTION);
	// try (Trx txRead = new Trx(MeshSpringConfiguration.getMeshSpringConfiguration().database())) {
	// vertex.reload();
	// transformAndResponde(rc, vertex);
	// }
	// }
	// hasFinished.set(true);
	// });
	// }
	// } catch (OConcurrentModificationException e) {
	// log.error("Creation failed in try {" + i + "} retrying.");
	// }
	// }
	// if (!hasFinished.get()) {
	// log.error("Creation failed after {" + RETRY_COUNT + "} attempts.");
	// rc.fail(new HttpStatusCodeErrorException(INTERNAL_SERVER_ERROR, "Creation failed after {" + RETRY_COUNT + "} attepmts."));
	// }
	//// } , false, rh -> {
	//// if (rh.failed()) {
	//// rc.fail(rh.cause());
	//// }
	//// });
	//
	// }

	public static <T extends GenericVertex<?>> void updateObject(ActionContext ac, String uuidParameterName, RootVertex<T> root) {
		Database db = MeshSpringConfiguration.getMeshSpringConfiguration().database();
		loadObject(ac, uuidParameterName, UPDATE_PERM, root, rh -> {
			if (hasSucceeded(ac, rh)) {
				GenericVertex<?> vertex = rh.result();
				vertex.update(ac, rh2 -> {
					if (rh2.failed()) {
						ac.fail(rh2.cause());
					} else {
						// Transform the vertex using a fresh transaction in order to start with a clean cache
						try (Trx txi = db.trx()) {
							vertex.reload();
							transformAndResponde(ac, vertex);
						}
					}
				});
			}
		});
	}

	public static <T extends GenericVertex<? extends RestModel>> void deleteObject(ActionContext ac, String uuidParameterName, String i18nMessageKey,
			RootVertex<T> root) {
		Database db = MeshSpringConfiguration.getMeshSpringConfiguration().database();

		loadObject(ac, uuidParameterName, DELETE_PERM, root, rh -> {
			if (hasSucceeded(ac, rh)) {
				GenericVertex<?> vertex = rh.result();
				String uuid = vertex.getUuid();
				String name = null;
				if (vertex instanceof NamedVertex) {
					name = ((NamedVertex) vertex).getName();
				}
				SearchQueueBatch batch = null;
				try (Trx txDelete = db.trx()) {
					if (vertex instanceof IndexedVertex) {
						batch = ((IndexedVertex) vertex).addIndexBatch(SearchQueueEntryAction.DELETE_ACTION);
					}
					vertex.delete();
					txDelete.success();
				}
				String id = name != null ? uuid + "/" + name : uuid;
				VerticleHelper.processOrFail2(ac, batch, brh -> {
					ac.send(toJson(new GenericMessageResponse(ac.i18n(i18nMessageKey, id))));
				});
			}
		});
	}

	public static <T extends GenericVertex<?>> void loadObject(ActionContext ac, String uuidParameterName, GraphPermission perm, RootVertex<T> root,
			Handler<AsyncResult<T>> handler) {

		String uuid = ac.getParameter(uuidParameterName);
		if (StringUtils.isEmpty(uuid)) {
			handler.handle(Future
					.failedFuture(new HttpStatusCodeErrorException(BAD_REQUEST, ac.i18n("error_request_parameter_missing", uuidParameterName))));
		} else {
			loadObjectByUuid(ac, uuid, perm, root, handler);
		}
	}

	/**
	 * Return the object with the given uuid if found within the specified root vertex. This method will not return null. Instead a
	 * {@link HttpStatusCodeErrorException} will be thrown when the object could not be found.
	 * 
	 * @param ac
	 * @param uuid
	 * @param perm
	 * @param root
	 * @return
	 */
	public static <T extends GenericVertex<?>> T loadObjectByUuidBlocking(ActionContext ac, String uuid, GraphPermission perm, RootVertex<T> root) {
		if (root == null) {
			throw new HttpStatusCodeErrorException(BAD_REQUEST, ac.i18n("error_root_node_not_found"));
		} else {

			T object = root.findByUuidBlocking(uuid);
			if (object == null) {
				throw new EntityNotFoundException(ac.i18n("object_not_found_for_uuid", uuid));
			} else {
				MeshAuthUser requestUser = ac.getUser();
				if (requestUser.hasPermission(object, perm)) {
					return object;
				} else {
					throw new InvalidPermissionException(ac.i18n("error_missing_perm", object.getUuid()));
				}

			}
		}
	}

	public static <T extends GenericVertex<?>> void loadObjectByUuid(ActionContext ac, String uuid, GraphPermission perm, RootVertex<T> root,
			Handler<AsyncResult<T>> handler) {
		if (root == null) {
			throw new HttpStatusCodeErrorException(BAD_REQUEST, ac.i18n("error_root_node_not_found"));
		} else {
			// try (Trx tx = MeshSpringConfiguration.getMeshSpringConfiguration().database().trx()) {
			// root.reload();
			// User user = getUser(rc);
			// user.reload();
			// T element = root.findByUuidBlocking(uuid);
			// if (user.hasPermission(element, perm)) {
			// System.out.println("JOW" + element.getUuid());
			// } else {
			// System.out.println("NÖ" + element.getUuid());
			// }
			// }
			root.findByUuid(uuid, rh -> {
				try (Trx tx = MeshSpringConfiguration.getMeshSpringConfiguration().database().trx()) {
					if (rh.failed()) {
						handler.handle(Future.failedFuture(rh.cause()));
					} else {
						T node = rh.result();
						if (node == null) {
							handler.handle(Future.failedFuture(new EntityNotFoundException(ac.i18n("object_not_found_for_uuid", uuid))));
						} else {
							MeshAuthUser requestUser = ac.getUser();
							if (requestUser.hasPermission(node, perm)) {
								handler.handle(Future.succeededFuture(node));
							} else {
								handler.handle(Future.failedFuture(new InvalidPermissionException(ac.i18n("error_missing_perm", node.getUuid()))));
							}
						}
					}
				}
			});
		}
	}

	public static boolean hasSucceeded(ActionContext ac, AsyncResult<?> result) {
		if (result.failed()) {
			ac.fail(result.cause());
			return false;
		}
		return true;
	}

}
