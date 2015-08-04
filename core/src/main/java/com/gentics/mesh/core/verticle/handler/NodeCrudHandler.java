package com.gentics.mesh.core.verticle.handler;

import static com.gentics.mesh.core.data.relationship.Permission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.READ_PERM;
import static com.gentics.mesh.core.data.search.SearchQueue.SEARCH_QUEUE_ENTRY_ADDRESS;
import static com.gentics.mesh.util.RoutingContextHelper.getUser;
import static com.gentics.mesh.util.VerticleHelper.delete;
import static com.gentics.mesh.util.VerticleHelper.hasSucceeded;
import static com.gentics.mesh.util.VerticleHelper.loadObject;
import static com.gentics.mesh.util.VerticleHelper.loadObjectByUuid;
import static com.gentics.mesh.util.VerticleHelper.loadTransformAndResponde;
import static com.gentics.mesh.util.VerticleHelper.transformAndResponde;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaReferenceInfo;
import com.gentics.mesh.error.EntityNotFoundException;
import com.gentics.mesh.error.InvalidPermissionException;
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.util.BlueprintTransaction;

@Component
public class NodeCrudHandler extends AbstractCRUDHandler {

	@Autowired
	private ServerSchemaStorage schemaStorage;

	@Override
	public void handleCreate(RoutingContext rc) {
		Project project = getProject(rc);
		MeshAuthUser requestUser = getUser(rc);

		String body = rc.getBodyAsString();
		SchemaReferenceInfo schemaInfo;
		try {
			schemaInfo = JsonUtil.readValue(body, SchemaReferenceInfo.class);
		} catch (Exception e) {
			rc.fail(e);
			return;
		}

		boolean missingSchemaInfo = schemaInfo.getSchema() == null
				|| (StringUtils.isEmpty(schemaInfo.getSchema().getUuid()) && StringUtils.isEmpty(schemaInfo.getSchema().getName()));
		if (missingSchemaInfo) {
			rc.fail(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "error_schema_parameter_missing")));
			return;
		}

		Handler<AsyncResult<SchemaContainer>> containerFoundHandler = rh -> {
			/*
			 * SchemaContainer schema = boot.schemaContainerRoot().findByName(requestModel.getSchema().getName()); if (schema == null) { rc.fail(new
			 * HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "schema_not_found", requestModel.getSchema() .getName()))); return; }
			 */
			SchemaContainer schemaContainer = rh.result();
			try {
				Schema schema = schemaContainer.getSchema();
				NodeCreateRequest requestModel = JsonUtil.readNode(body, NodeCreateRequest.class, schemaStorage);
				if (StringUtils.isEmpty(requestModel.getParentNodeUuid())) {
					rc.fail(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "node_missing_parentnode_field")));
					return;
				}
				if (StringUtils.isEmpty(requestModel.getLanguage())) {
					rc.fail(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "node_no_languagecode_specified")));
					return;
				}

				Handler<AsyncResult<Node>> handler = ch -> {
					if (hasSucceeded(rc, ch)) {
						Node node = ch.result();
						transformAndResponde(rc, node);
					}
				};
				Future<Node> nodeCreated = Future.future();
				nodeCreated.setHandler(handler);

				Handler<AsyncResult<Node>> nodeCreatedHandler = pnh -> {
					Node node = pnh.result();
					try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {

						Language language = boot.languageRoot().findByLanguageTag(requestModel.getLanguage());
						if (language == null) {
							nodeCreated.fail(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "node_no_language_found",
									requestModel.getLanguage())));
						} else {
							try {
								NodeFieldContainer container = node.getOrCreateFieldContainer(language);
								container.setFieldFromRest(rc, requestModel.getFields(), schema);

								//Inform elasticsearch about the new element
								searchQueue.put(node.getUuid(), Node.TYPE, SearchQueueEntryAction.CREATE_ACTION);
								vertx.eventBus().send(SEARCH_QUEUE_ENTRY_ADDRESS, null);
								tx.success();
								nodeCreated.complete(node);
							} catch (Exception e) {
								rc.fail(e);
								return;
							}
						}
					}
				};

				loadObjectByUuid(rc, requestModel.getParentNodeUuid(), CREATE_PERM, project.getNodeRoot(), rhp -> {
					if (hasSucceeded(rc, rhp)) {
						Node parentNode = rhp.result();
						Node node = parentNode.create(requestUser, schemaContainer, project);
						requestUser.addCRUDPermissionOnRole(parentNode, CREATE_PERM, node);
						nodeCreatedHandler.handle(Future.succeededFuture(node));
					}
				});
			} catch (Exception e) {
				rc.fail(e);
				return;
			}
		};
		if (!StringUtils.isEmpty(schemaInfo.getSchema().getName())) {
			SchemaContainer containerByName = project.getSchemaRoot().findByName(schemaInfo.getSchema().getName());
			if (containerByName != null) {
				if (requestUser.hasPermission(containerByName, READ_PERM)) {
					containerFoundHandler.handle(Future.succeededFuture(containerByName));
				} else {
					rc.fail(new InvalidPermissionException(i18n.get(rc, "error_missing_perm", containerByName.getUuid())));
				}
			} else {
				rc.fail(new EntityNotFoundException(i18n.get(rc, "schema_not_found", schemaInfo.getSchema().getName())));
			}
		} else {
			loadObjectByUuid(rc, schemaInfo.getSchema().getUuid(), READ_PERM, project.getSchemaRoot(), rh -> {
				if (hasSucceeded(rc, rh)) {
					SchemaContainer schemaContainer = rh.result();
					containerFoundHandler.handle(Future.succeededFuture(schemaContainer));
				}
			});
		}

	}

	@Override
	public void handleDelete(RoutingContext rc) {
		String uuid = rc.request().params().get("uuid");
		Project project = getProject(rc);
		if (project.getBaseNode().getUuid().equals(uuid)) {
			rc.fail(new HttpStatusCodeErrorException(METHOD_NOT_ALLOWED, i18n.get(rc, "node_basenode_not_deletable")));
		} else {
			delete(rc, "uuid", "node_deleted", getProject(rc).getNodeRoot());
		}
	}

	@Override
	public void handleUpdate(RoutingContext rc) {
		NodeUpdateRequest requestModel;
		try {
			requestModel = JsonUtil.readNode(rc.getBodyAsString(), NodeUpdateRequest.class, schemaStorage);
			if (StringUtils.isEmpty(requestModel.getLanguage())) {
				rc.fail(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "error_language_not_set")));
				return;
			}
			Project project = getProject(rc);
			loadObject(
					rc,
					"uuid",
					READ_PERM,
					project.getNodeRoot(),
					rh -> {
						if (hasSucceeded(rc, rh)) {
							Node node = rh.result();
							try {
								Language language = boot.languageRoot().findByLanguageTag(requestModel.getLanguage());
								if (language == null) {
									rc.fail(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "error_language_not_found",
											requestModel.getLanguage())));
									return;
								}
								/* TODO handle other fields, node.setEditor(requestUser); etc. */
								try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
									NodeFieldContainer container = node.getOrCreateFieldContainer(language);
									Schema schema = node.getSchema();
									try {
										container.setFieldFromRest(rc, requestModel.getFields(), schema);
										searchQueue.put(node.getUuid(), Node.TYPE, SearchQueueEntryAction.UPDATE_ACTION);
										vertx.eventBus().send(SEARCH_QUEUE_ENTRY_ADDRESS, null);
										tx.success();
										transformAndResponde(rc, node);
									} catch (MeshSchemaException e) {
										tx.failure();
										/* TODO i18n */
										rc.fail(new HttpStatusCodeErrorException(BAD_REQUEST, e.getMessage()));
									}
								}

							} catch (IOException e) {
								rc.fail(e);
							}
						}
					});
		} catch (Exception e1) {
			rc.fail(e1);
		}
	}

	@Override
	public void handleRead(RoutingContext rc) {
		String uuid = rc.request().params().get("uuid");
		if (StringUtils.isEmpty(uuid)) {
			rc.next();
		} else {
			Project project = getProject(rc);
			loadTransformAndResponde(rc, "uuid", READ_PERM, project.getNodeRoot());
		}
	}

	@Override
	public void handleReadList(RoutingContext rc) {
		Project project = getProject(rc);
		loadTransformAndResponde(rc, project.getNodeRoot(), new NodeListResponse());
	}

}
