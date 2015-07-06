package com.gentics.mesh.core.verticle;

import static com.gentics.mesh.core.data.relationship.Permission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.UPDATE_PERM;
import static com.gentics.mesh.json.JsonUtil.toJson;
import static com.gentics.mesh.util.RoutingContextHelper.getPagingInfo;
import static com.gentics.mesh.util.RoutingContextHelper.getSelectedLanguageTags;
import static com.gentics.mesh.util.RoutingContextHelper.getUser;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.ext.web.Route;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.AbstractProjectRestVerticle;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.impl.TagImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.relationship.Permission;
import com.gentics.mesh.core.data.service.transformation.TransformationInfo;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaReferenceInfo;
import com.gentics.mesh.core.verticle.handler.NodeListHandler;
import com.gentics.mesh.core.verticle.handler.TagListHandler;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.util.RestModelPagingHelper;

/**
 * The content verticle adds rest endpoints for manipulating nodes.
 */
@Component
@Scope("singleton")
@SpringVerticle
public class NodeVerticle extends AbstractProjectRestVerticle {

	//	private static final Logger log = LoggerFactory.getLogger(MeshNodeVerticle.class);

	@Autowired
	private TagListHandler tagListHandler;

	@Autowired
	private NodeListHandler nodeListHandler;

	@Autowired
	private BootstrapInitializer boot;

	public NodeVerticle() {
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
			nodeListHandler.handleNodeList(rc, (projectName, parentNode, languageTags, pagingInfo) -> {
				return parentNode.getChildren(requestUser, projectName, languageTags, pagingInfo);
			});
		});

	}

	// TODO filtering, sorting
	private void addTagsHandler() {
		Route getRoute = route("/:uuid/tags").method(GET).produces(APPLICATION_JSON);
		getRoute.handler(rc -> {
			MeshAuthUser requestUser = getUser(rc);
			tagListHandler.handle(rc, (projectName, node, languageTags, pagingInfo) -> {
				return node.getTags(requestUser, projectName, languageTags, pagingInfo);
			});
		});

		Route postRoute = route("/:uuid/tags/:tagUuid").method(POST).produces(APPLICATION_JSON);
		postRoute.handler(rc -> {
			String projectName = rcs.getProjectName(rc);
			MeshAuthUser requestUser = getUser(rc);
			List<String> languageTags = getSelectedLanguageTags(rc);

			rcs.loadObject(rc, "uuid", projectName, UPDATE_PERM, NodeImpl.class, (AsyncResult<Node> rh) -> {
				rcs.loadObject(rc, "tagUuid", projectName, READ_PERM, TagImpl.class, (AsyncResult<Tag> srh) -> {
					Node node = rh.result();
					Tag tag = srh.result();
					node.addTag(tag);
				}, trh -> {
					Node node = rh.result();
					TransformationInfo info = new TransformationInfo(requestUser, languageTags, rc);
					rc.response().setStatusCode(200).end(node.getNodeResponseJson(info));
				});

			});
		});

		// TODO fix error handling. This does not fail when tagUuid could not be found
		Route deleteRoute = route("/:uuid/tags/:tagUuid").method(DELETE).produces(APPLICATION_JSON);
		deleteRoute.handler(rc -> {
			String projectName = rcs.getProjectName(rc);
			MeshAuthUser requestUser = getUser(rc);
			List<String> languageTags = getSelectedLanguageTags(rc);

			rcs.loadObject(rc, "uuid", projectName, UPDATE_PERM, NodeImpl.class, (AsyncResult<Node> rh) -> {
				rcs.loadObject(rc, "tagUuid", projectName, READ_PERM, TagImpl.class, (AsyncResult<Tag> srh) -> {
					Node node = rh.result();
					Tag tag = srh.result();
					node.removeTag(tag);
				}, trh -> {
					Node node = rh.result();
					TransformationInfo info = new TransformationInfo(requestUser, languageTags, rc);
					rc.response().setStatusCode(200).end(node.getNodeResponseJson(info));
				});
			});

		});
	}

	// TODO maybe projects should not be a set?
	// TODO handle schema by name / by uuid - move that code in a seperate
	// handler
	// TODO load the schema and set the reference to the tag
	private void addCreateHandler() {
		Route route = route("/").method(POST);
		route.handler(rc -> {
			String projectName = rcs.getProjectName(rc);
			MeshAuthUser requestUser = getUser(rc);
			List<String> languageTags = getSelectedLanguageTags(rc);

			String body = rc.getBodyAsString();
			SchemaReferenceInfo schemaInfo;
			try {
				schemaInfo = JsonUtil.readValue(body, SchemaReferenceInfo.class);
			} catch (Exception e) {
				rc.fail(e);
				return;
			}

			if (schemaInfo.getSchema() == null || StringUtils.isEmpty(schemaInfo.getSchema().getName()) || StringUtils.isEmpty(schemaInfo.getSchema().getUuid())) {
				rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "error_schema_parameter_missing")));
				return;
			}

			rcs.loadObjectByUuid(rc, schemaInfo.getSchema().getUuid(), Permission.READ_PERM, SchemaContainerImpl.class, (
					AsyncResult<SchemaContainer> rh) -> {

				SchemaContainer schemaContainer = rh.result();

				/*
				 * SchemaContainer schema = boot.schemaContainerRoot().findByName(requestModel.getSchema().getName()); if (schema == null) { rc.fail(new
				 * HttpStatusCodeErrorException(400, i18n.get(rc, "schema_not_found", requestModel.getSchema() .getName()))); return; }
				 */

				try {
					Schema schema = schemaContainer.getSchema();
					NodeCreateRequest requestModel = JsonUtil.readNode(body, NodeCreateRequest.class, schema);
					if (StringUtils.isEmpty(requestModel.getParentNodeUuid())) {
						rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "node_missing_parentnode_field")));
						return;
					}

					Future<Node> contentCreated = Future.future();

					rcs.loadObjectByUuid(rc, requestModel.getParentNodeUuid(), projectName, CREATE_PERM, NodeImpl.class, (AsyncResult<Node> rhp) -> {

						Node parentNode = rhp.result();
						Node node = parentNode.create();

						node.setSchemaContainer(schemaContainer);

						node.setCreator(requestUser);

						Project project = boot.projectRoot().findByName(projectName);
						node.addProject(project);

						requestUser.addCRUDPermissionOnRole(parentNode, CREATE_PERM, node);

						/* Assign the content to the tag and save the tag */
						//rootTagForContent.(content);

							contentCreated.complete(node);
						}, trh -> {
							if (trh.failed()) {
								rc.fail(trh.cause());
							}
							Node node = contentCreated.result();
							TransformationInfo info = new TransformationInfo(requestUser, languageTags, rc);
							rc.response().setStatusCode(200).end(node.getNodeResponseJson(info));
						});
				} catch (Exception e) {
					rc.fail(e);
					return;
				}

			});

		});
	}

	// TODO filter by project name
	// TODO filtering
	private void addReadHandler() {
		Route route = route("/:uuid").method(GET).produces(APPLICATION_JSON);
		route.handler(rc -> {
			String projectName = rcs.getProjectName(rc);
			MeshAuthUser requestUser = getUser(rc);
			List<String> languageTags = getSelectedLanguageTags(rc);

			rcs.loadObject(rc, "uuid", projectName, READ_PERM, NodeImpl.class, (AsyncResult<Node> rh) -> {
			}, trh -> {
				if (trh.failed()) {
					rc.fail(trh.cause());
				}
				Node node = trh.result();
				TransformationInfo info = new TransformationInfo(requestUser, languageTags, rc);
				rc.response().setStatusCode(200).end(node.getNodeResponseJson(info));
			});

		});

		Route readAllRoute = route().method(GET).produces(APPLICATION_JSON);
		readAllRoute.handler(rc -> {
			String projectName = rcs.getProjectName(rc);
			MeshAuthUser requestUser = getUser(rc);

			List<String> languageTags = getSelectedLanguageTags(rc);
			PagingInfo pagingInfo = getPagingInfo(rc);

			vertx.executeBlocking((Future<NodeListResponse> bch) -> {
				NodeListResponse listResponse = new NodeListResponse();
				Page<? extends Node> nodePage;
				try {
					TransformationInfo info = new TransformationInfo(requestUser, languageTags, rc);
					nodePage = boot.nodeRoot().findAll(requestUser, projectName, languageTags, pagingInfo);
					for (Node node : nodePage) {
						listResponse.getData().add(node.transformToRest(info));
					}
					RestModelPagingHelper.setPaging(listResponse, nodePage, pagingInfo);
					bch.complete(listResponse);
				} catch (Exception e) {
					bch.fail(e);
				}

			}, arh -> {
				if (arh.failed()) {
					rc.fail(arh.cause());
				}
				NodeListResponse listResponse = arh.result();
				rc.response().setStatusCode(200).end(toJson(listResponse));
			});
		});
	}

	// TODO filter project name
	private void addDeleteHandler() {
		Route route = route("/:uuid").method(DELETE).produces(APPLICATION_JSON);
		route.handler(rc -> {
			String projectName = rcs.getProjectName(rc);
			rcs.loadObject(rc, "uuid", projectName, DELETE_PERM, NodeImpl.class, (AsyncResult<Node> rh) -> {
				Node node = rh.result();
				node.delete();
			}, trh -> {
				if (trh.failed()) {
					rc.fail(trh.cause());
				}
				String uuid = rc.request().params().get("uuid");
				rc.response().setStatusCode(200).end(toJson(new GenericMessageResponse(i18n.get(rc, "node_deleted", uuid))));
			});
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
			String projectName = rcs.getProjectName(rc);
			List<String> languageTags = getSelectedLanguageTags(rc);
			MeshAuthUser requestUser = getUser(rc);

			rcs.loadObject(rc, "uuid", projectName, READ_PERM, NodeImpl.class, (AsyncResult<Node> rh) -> {
				Node content = rh.result();

				try {
					NodeUpdateRequest request = JsonUtil.readNode(rc.getBodyAsString(), NodeUpdateRequest.class, content.getSchema());
					// Iterate through all properties and update the changed
					// ones
					//					for (String languageTag : request.getProperties().keySet()) {
					//						Language language = languageRoot.findByLanguageTag(languageTag);
					//						if (language != null) {
					//							languageTags.add(languageTag);
					//							Map<String, String> properties = request.getProperties();
					//							if (properties != null) {
					//								I18NProperties i18nProperties = content.getI18nProperties(language);
					//								for (Map.Entry<String, String> set : properties.entrySet()) {
					//									String key = set.getKey();
					//									String value = set.getValue();
					//									String i18nValue = i18nProperties.getProperty(key);
					//									/*
					//									 * Tag does not have the value so lets create it
					//									 */
					//									if (i18nValue == null) {
					//										i18nProperties.setProperty(key, value);
					//									} else {
					//										/*
					//										 * Lets compare and update if the value has changed
					//										 */
					//										if (!value.equals(i18nValue)) {
					//											i18nProperties.setProperty(key, value);
					//										}
					//									}
					//								}
					//
					//							}
					//						} else {
					//							rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "error_language_not_found", languageTag)));
					//							return;
					//						}
					//
					//					}
				} catch (IOException e) {
					rc.fail(e);
					return;
				}

			}, trh -> {
				if (trh.failed()) {
					rc.fail(trh.cause());
				}
				Node node = trh.result();
				TransformationInfo info = new TransformationInfo(requestUser, languageTags, rc);
				rc.response().setStatusCode(200).end(toJson(node.transformToRest(info)));
			});

		});
	}
}
