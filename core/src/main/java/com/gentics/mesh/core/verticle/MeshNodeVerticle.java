package com.gentics.mesh.core.verticle;

import static com.gentics.mesh.core.data.model.relationship.Permission.CREATE_PERM;
import static com.gentics.mesh.core.data.model.relationship.Permission.DELETE_PERM;
import static com.gentics.mesh.core.data.model.relationship.Permission.READ_PERM;
import static com.gentics.mesh.core.data.model.relationship.Permission.UPDATE_PERM;
import static com.gentics.mesh.util.JsonUtils.fromJson;
import static com.gentics.mesh.util.JsonUtils.toJson;
import static com.gentics.mesh.util.RoutingContextHelper.getUser;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.ext.web.Route;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jacpfx.vertx.spring.SpringVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.auth.MeshPermission;
import com.gentics.mesh.core.AbstractProjectRestVerticle;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.model.tinkerpop.I18NProperties;
import com.gentics.mesh.core.data.model.tinkerpop.Language;
import com.gentics.mesh.core.data.model.tinkerpop.MeshNode;
import com.gentics.mesh.core.data.model.tinkerpop.MeshShiroUser;
import com.gentics.mesh.core.data.model.tinkerpop.MeshUser;
import com.gentics.mesh.core.data.model.tinkerpop.Project;
import com.gentics.mesh.core.data.model.tinkerpop.Schema;
import com.gentics.mesh.core.data.model.tinkerpop.Tag;
import com.gentics.mesh.core.rest.common.response.GenericMessageResponse;
import com.gentics.mesh.core.rest.node.request.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.request.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.response.NodeListResponse;
import com.gentics.mesh.core.verticle.handler.MeshNodeListHandler;
import com.gentics.mesh.core.verticle.handler.TagListHandler;
import com.gentics.mesh.error.HttpStatusCodeErrorException;
import com.gentics.mesh.paging.PagingInfo;
import com.gentics.mesh.util.RestModelPagingHelper;

/**
 * The content verticle adds rest endpoints for manipulating contents.
 */
@Component
@Scope("singleton")
@SpringVerticle
public class MeshNodeVerticle extends AbstractProjectRestVerticle {

	private static final Logger log = LoggerFactory.getLogger(MeshNodeVerticle.class);

	@Autowired
	private TagListHandler tagListHandler;

	@Autowired
	private MeshNodeListHandler nodeListHandler;

	public MeshNodeVerticle() {
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
			MeshShiroUser requestUser = getUser(rc);
			nodeListHandler.handleNodeList(rc, (projectName, parentNode, languageTags, pagingInfo) -> {
				return parentNode.getChildren(requestUser, projectName, languageTags, pagingInfo);
			});
		});

	}

	// TODO filtering, sorting
	private void addTagsHandler() {
		Route getRoute = route("/:uuid/tags").method(GET).produces(APPLICATION_JSON);
		getRoute.handler(rc -> {
			MeshShiroUser requestUser = getUser(rc);
			tagListHandler.handle(rc, (projectName, node, languageTags, pagingInfo) -> {
				return node.getTags(requestUser, projectName, languageTags, pagingInfo);
			});
		});

		Route postRoute = route("/:uuid/tags/:tagUuid").method(POST).produces(APPLICATION_JSON);
		postRoute.handler(rc -> {
			String projectName = rcs.getProjectName(rc);
			MeshShiroUser requestUser = getUser(rc);

			rcs.loadObject(rc, "uuid", projectName, UPDATE_PERM, (AsyncResult<MeshNode> rh) -> {
				rcs.loadObject(rc, "tagUuid", projectName, READ_PERM, (AsyncResult<Tag> srh) -> {
					MeshNode node = rh.result();
					Tag tag = srh.result();
					node.addTag(tag);
				}, trh -> {
					MeshNode node = rh.result();
					rc.response().setStatusCode(200).end(toJson(node.transformToRest(requestUser)));
				});

			});
		});

		// TODO fix error handling. This does not fail when tagUuid could not be found
		Route deleteRoute = route("/:uuid/tags/:tagUuid").method(DELETE).produces(APPLICATION_JSON);
		deleteRoute.handler(rc -> {
			String projectName = rcs.getProjectName(rc);
			MeshShiroUser requestUser = getUser(rc);

			rcs.loadObject(rc, "uuid", projectName, UPDATE_PERM, (AsyncResult<MeshNode> rh) -> {
				rcs.loadObject(rc, "tagUuid", projectName, READ_PERM, (AsyncResult<Tag> srh) -> {
					MeshNode node = rh.result();
					Tag tag = srh.result();
					node.removeTag(tag);
				}, trh -> {
					MeshNode node = rh.result();
					rc.response().setStatusCode(200).end(toJson(node.transformToRest(requestUser)));
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
			MeshShiroUser requestUser = getUser(rc);

			NodeCreateRequest requestModel = fromJson(rc, NodeCreateRequest.class);

			if (StringUtils.isEmpty(requestModel.getParentNodeUuid())) {
				rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "node_missing_parentnode_field")));
				return;
			}

			Future<MeshNode> contentCreated = Future.future();

			rcs.loadObjectByUuid(rc, requestModel.getParentNodeUuid(), projectName, CREATE_PERM, (AsyncResult<MeshNode> rh) -> {

				MeshNode rootNodeForContent = rh.result();
				MeshNode node = nodeService.create();

				if (requestModel.getSchema() == null || StringUtils.isEmpty(requestModel.getSchema().getSchemaName())) {
					rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "error_schema_parameter_missing")));
					return;
				} else {

					Schema schema = schemaService.findByName(requestModel.getSchema().getSchemaName());
					if (schema == null) {
						rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "schema_not_found", requestModel.getSchema().getSchemaName())));
						return;
					} else {
						node.setSchema(schema);
					}
				}

				node.setCreator(requestUser);

				Project project = projectService.findByName(projectName);
				node.addProject(project);

				/* Add the i18n properties to the newly created tag */
				for (String languageTag : requestModel.getProperties().keySet()) {
					Map<String, String> i18nProperties = requestModel.getProperties();
					Language language = languageService.findByLanguageTag(languageTag);
					if (language == null) {
						rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "error_language_not_found", languageTag)));
						return;
					}

					I18NProperties tagProps = node.getOrCreateI18nProperties(language);
					for (Map.Entry<String, String> entry : i18nProperties.entrySet()) {
						tagProps.setProperty(entry.getKey(), entry.getValue());
					}
				}

				roleService.addCRUDPermissionOnRole(requestUser, new MeshPermission(rootNodeForContent, CREATE_PERM), node);

				/* Assign the content to the tag and save the tag */
				//				rootTagForContent.(content);

					contentCreated.complete(node);
				}, trh -> {
					if (trh.failed()) {
						rc.fail(trh.cause());
					}
					MeshNode node = contentCreated.result();
					rc.response().setStatusCode(200).end(toJson(node.transformToRest(requestUser)));
				});

		});
	}

	// TODO filter by project name
	// TODO filtering
	private void addReadHandler() {
		Route route = route("/:uuid").method(GET).produces(APPLICATION_JSON);
		route.handler(rc -> {
			String projectName = rcs.getProjectName(rc);
			MeshShiroUser requestUser = getUser(rc);

			rcs.loadObject(rc, "uuid", projectName, READ_PERM, (AsyncResult<MeshNode> rh) -> {
			}, trh -> {
				if (trh.failed()) {
					rc.fail(trh.cause());
				}
				MeshNode node = trh.result();
				rc.response().setStatusCode(200).end(toJson(node.transformToRest(requestUser)));
			});

		});

		Route readAllRoute = route().method(GET).produces(APPLICATION_JSON);
		readAllRoute.handler(rc -> {
			String projectName = rcs.getProjectName(rc);
			MeshShiroUser requestUser = getUser(rc);

			List<String> languageTags = rcs.getSelectedLanguageTags(rc);
			PagingInfo pagingInfo = rcs.getPagingInfo(rc);

			vertx.executeBlocking((Future<NodeListResponse> bch) -> {
				NodeListResponse listResponse = new NodeListResponse();
				Page<? extends MeshNode> nodePage;
				try {
					nodePage = nodeService.findAll(requestUser, projectName, languageTags, pagingInfo);
					for (MeshNode content : nodePage) {
						listResponse.getData().add(content.transformToRest(requestUser));
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
			rcs.loadObject(rc, "uuid", projectName, DELETE_PERM, (AsyncResult<MeshNode> rh) -> {
				MeshNode node = rh.result();
				nodeService.delete(node);
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
			List<String> languageTags = rcs.getSelectedLanguageTags(rc);
			MeshShiroUser requestUser = getUser(rc);

			rcs.loadObject(rc, "uuid", projectName, READ_PERM, (AsyncResult<MeshNode> rh) -> {
				MeshNode content = rh.result();

				NodeUpdateRequest request = fromJson(rc, NodeUpdateRequest.class);
				// Iterate through all properties and update the changed
				// ones
					for (String languageTag : request.getProperties().keySet()) {
						Language language = languageService.findByLanguageTag(languageTag);
						if (language != null) {
							languageTags.add(languageTag);
							Map<String, String> properties = request.getProperties();
							if (properties != null) {
								I18NProperties i18nProperties = content.getI18nProperties(language);
								for (Map.Entry<String, String> set : properties.entrySet()) {
									String key = set.getKey();
									String value = set.getValue();
									String i18nValue = i18nProperties.getProperty(key);
									/*
									 * Tag does not have the value so lets create it
									 */
									if (i18nValue == null) {
										i18nProperties.setProperty(key, value);
									} else {
										/*
										 * Lets compare and update if the value has changed
										 */
										if (!value.equals(i18nValue)) {
											i18nProperties.setProperty(key, value);
										}
									}
								}

							}
						} else {
							rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "error_language_not_found", languageTag)));
							return;
						}

					}
				}, trh -> {
					if (trh.failed()) {
						rc.fail(trh.cause());
					}
					MeshNode node = trh.result();
					rc.response().setStatusCode(200).end(toJson(node.transformToRest(requestUser)));
				});

		});
	}

}
