package com.gentics.mesh.core.verticle.project;

import static com.gentics.mesh.core.data.relationship.Permission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.UPDATE_PERM;
import static com.gentics.mesh.util.RoutingContextHelper.getPagingInfo;
import static com.gentics.mesh.util.RoutingContextHelper.getSelectedLanguageTags;
import static com.gentics.mesh.util.RoutingContextHelper.getUser;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.Route;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.AbstractProjectRestVerticle;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaReferenceInfo;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.error.EntityNotFoundException;
import com.gentics.mesh.error.InvalidPermissionException;
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.util.BlueprintTransaction;

/**
 * The content verticle adds rest endpoints for manipulating nodes.
 */
@Component
@Scope("singleton")
@SpringVerticle
public class ProjectNodeVerticle extends AbstractProjectRestVerticle {

	// private static final Logger log = LoggerFactory.getLogger(MeshNodeVerticle.class);

	@Autowired
	private ServerSchemaStorage schemaStorage;

	@Autowired
	private BootstrapInitializer boot;

	public ProjectNodeVerticle() {
		super("nodes");
	}

	@Override
	public void registerEndPoints() throws Exception {
		route("/*").handler(springConfiguration.authHandler());
		addCreateHandler();
		addReadHandler();
		addUpdateHandler();
		addDeleteHandler();
		addChildrenHandler();
		addTagsHandler();
	}

	private void addChildrenHandler() {
		Route getRoute = route("/:uuid/children").method(GET).produces(APPLICATION_JSON);
		getRoute.handler(rc -> {
			MeshAuthUser requestUser = getUser(rc);
			Project project = getProject(rc);
			loadObject(rc, "uuid", READ_PERM, project.getNodeRoot(), rh -> {
				if (hasSucceeded(rc, rh)) {
					Node node = rh.result();
					Page<? extends Node> page = node.getChildren(requestUser, getSelectedLanguageTags(rc), getPagingInfo(rc));
					transformAndResponde(rc, page, new NodeListResponse());
				}
			});
		});

	}

	// TODO filtering, sorting
	private void addTagsHandler() {
		Route getRoute = route("/:uuid/tags").method(GET).produces(APPLICATION_JSON);
		getRoute.handler(rc -> {
			Project project = getProject(rc);
			MeshAuthUser requestUser = getUser(rc);
			loadObject(rc, "uuid", READ_PERM, project.getNodeRoot(), rh -> {
				if (hasSucceeded(rc, rh)) {
					Node node = rh.result();
					try {
						Page<? extends Tag> tagPage = node.getTags(requestUser, getPagingInfo(rc));
						transformAndResponde(rc, tagPage, new TagListResponse());
					} catch (Exception e) {
						rc.fail(e);
					}
				}
			});
		});

		Route postRoute = route("/:uuid/tags/:tagUuid").method(POST).produces(APPLICATION_JSON);
		postRoute.handler(rc -> {
			Project project = getProject(rc);
			if (project == null) {
				rc.fail(new HttpStatusCodeErrorException(400, "Project not found"));
				// TODO i18n error
			} else {
				loadObject(rc, "uuid", UPDATE_PERM, project.getNodeRoot(), rh -> {
					Node node = rh.result();
					loadObject(rc, "tagUuid", READ_PERM, project.getTagRoot(), th -> {
						if (hasSucceeded(rc, th)) {
							Tag tag = th.result();
							node.addTag(tag);
							transformAndResponde(rc, node);
						}
					});
				});

			}
		});

		// TODO fix error handling. This does not fail when tagUuid could not be found
		Route deleteRoute = route("/:uuid/tags/:tagUuid").method(DELETE).produces(APPLICATION_JSON);
		deleteRoute.handler(rc -> {
			Project project = getProject(rc);
			loadObject(rc, "uuid", UPDATE_PERM, project.getNodeRoot(), rh -> {
				loadObject(rc, "tagUuid", READ_PERM, project.getTagRoot(), srh -> {
					if (hasSucceeded(rc, srh) && hasSucceeded(rc, rh)) {
						Node node = rh.result();
						Tag tag = srh.result();
						node.removeTag(tag);
						transformAndResponde(rc, node);
					}
				});
			});

		});
	}

	// TODO handle schema by name / by uuid - move that code in a separate
	// handler
	private void addCreateHandler() {
		Route route = route("/").method(POST).produces(APPLICATION_JSON);
		route.handler(rc -> {
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
				rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "error_schema_parameter_missing")));
				return;
			}

			Handler<AsyncResult<SchemaContainer>> containerFoundHandler = rh -> {
				/*
				 * SchemaContainer schema = boot.schemaContainerRoot().findByName(requestModel.getSchema().getName()); if (schema == null) { rc.fail(new
				 * HttpStatusCodeErrorException(400, i18n.get(rc, "schema_not_found", requestModel.getSchema() .getName()))); return; }
				 */
				SchemaContainer schemaContainer = rh.result();
				try {
					Schema schema = schemaContainer.getSchema();
					NodeCreateRequest requestModel = JsonUtil.readNode(body, NodeCreateRequest.class, schemaStorage);
					if (StringUtils.isEmpty(requestModel.getParentNodeUuid())) {
						rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "node_missing_parentnode_field")));
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
								nodeCreated.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "node_no_language_found",
										requestModel.getLanguage())));
							} else {
								try {
									NodeFieldContainer container = node.getOrCreateFieldContainer(language);
									container.setFieldFromRest(rc, requestModel.getFields(), schema);
									tx.success();
									nodeCreated.complete(node);
								} catch (Exception e) {
									rc.fail(e);
									return;
								}
							}
						}
					};

					if (project.getBaseNode().getUuid().equals(requestModel.getParentNodeUuid())) {
						Node node = project.getBaseNode().create(requestUser, schemaContainer, project);
						requestUser.addCRUDPermissionOnRole(project.getBaseNode(), CREATE_PERM, node);
						nodeCreatedHandler.handle(Future.succeededFuture(node));
					} else {
						loadObjectByUuid(rc, requestModel.getParentNodeUuid(), CREATE_PERM, project.getNodeRoot(), rhp -> {
							if (hasSucceeded(rc, rhp)) {
								Node parentNode = rhp.result();
								Node node = project.getBaseNode().create(requestUser, schemaContainer, project);
								requestUser.addCRUDPermissionOnRole(parentNode, CREATE_PERM, node);
								nodeCreatedHandler.handle(Future.succeededFuture(node));
							}
						});
					}
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

		});
	}

	// TODO filter by project name
	// TODO filtering
	private void addReadHandler() {

		Route route = route("/:uuid").method(GET).produces(APPLICATION_JSON);
		route.handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			if (StringUtils.isEmpty(uuid)) {
				rc.next();
			} else {
				Project project = getProject(rc);
				loadTransformAndResponde(rc, "uuid", READ_PERM, project.getNodeRoot());
			}
		});

		Route readAllRoute = route("/").method(GET).produces(APPLICATION_JSON);
		readAllRoute.handler(rc -> {
			Project project = getProject(rc);
			loadTransformAndResponde(rc, project.getNodeRoot(), new NodeListResponse());
		});

	}

	// TODO filter project name
	private void addDeleteHandler() {
		Route route = route("/:uuid").method(DELETE).produces(APPLICATION_JSON);
		route.handler(rc -> {
			delete(rc, "uuid", "node_deleted", getProject(rc).getNodeRoot());
		});
	}

	// TODO filter by project name
	// TODO handle depth
	// TODO update other fields as well?
	// TODO Update user information
	// TODO use schema and only handle those i18n properties that were specified
	// within the schema.
	private void addUpdateHandler() {

		Route route = route("/:uuid").method(PUT).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		route.handler(rc -> {
			NodeUpdateRequest requestModel;
			try {
				requestModel = JsonUtil.readNode(rc.getBodyAsString(), NodeUpdateRequest.class, schemaStorage);
				if (StringUtils.isEmpty(requestModel.getLanguage())) {
					rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "error_language_not_set")));
					return;
				}
				Project project = getProject(rc);
				loadObject(rc, "uuid", READ_PERM, project.getNodeRoot(), rh -> {
					if (hasSucceeded(rc, rh)) {
						Node node = rh.result();
						try {
							Language language = boot.languageRoot().findByLanguageTag(requestModel.getLanguage());
							if (language == null) {
								rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "error_language_not_found", requestModel.getLanguage())));
								return;
							}
							/* TODO handle other fields, node.setEditor(requestUser); etc. */
							try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
								NodeFieldContainer container = node.getOrCreateFieldContainer(language);
								Schema schema = node.getSchema();
								try {
									container.setFieldFromRest(rc, requestModel.getFields(), schema);
									tx.success();
									transformAndResponde(rc, node);
								} catch (MeshSchemaException e) {
									tx.failure();
									/* TODO i18n */
									rc.fail(new HttpStatusCodeErrorException(400, e.getMessage()));
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

		});
	}
}
