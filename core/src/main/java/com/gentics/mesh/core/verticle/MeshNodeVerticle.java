package com.gentics.mesh.core.verticle;

import static com.gentics.mesh.util.JsonUtils.fromJson;
import static com.gentics.mesh.util.JsonUtils.toJson;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.ext.apex.Route;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jacpfx.vertx.spring.SpringVerticle;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractProjectRestVerticle;
import com.gentics.mesh.core.data.model.I18NProperties;
import com.gentics.mesh.core.data.model.Language;
import com.gentics.mesh.core.data.model.MeshNode;
import com.gentics.mesh.core.data.model.Project;
import com.gentics.mesh.core.data.model.Tag;
import com.gentics.mesh.core.data.model.auth.MeshPermission;
import com.gentics.mesh.core.data.model.auth.PermissionType;
import com.gentics.mesh.core.data.model.auth.User;
import com.gentics.mesh.core.data.model.relationship.Translated;
import com.gentics.mesh.core.data.model.schema.ObjectSchema;
import com.gentics.mesh.core.repository.ObjectSchemaRepository;
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
	private ObjectSchemaRepository objectSchemaRepository;

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

	private void addTagsHandler() {
		Route getRoute = route("/:uuid/children").method(GET).produces(APPLICATION_JSON);
		getRoute.handler(rc -> {
			nodeListHandler.handleNodeList(rc, (projectName, parentNode, languageTags, pagingInfo) -> {
				return nodeService.findChildren(rc, projectName, parentNode, languageTags, pagingInfo);
			});
		});

		Route postRoute = route("/:uuid/children/:nodeChildUuid").method(POST).produces(APPLICATION_JSON);
		postRoute.handler(rc -> {
			String projectName = rcs.getProjectName(rc);
			rcs.loadObject(rc, "uuid", projectName, PermissionType.UPDATE, (AsyncResult<MeshNode> rh) -> {
				rcs.loadObject(rc, "nodeChildUuid", projectName, PermissionType.READ, (AsyncResult<MeshNode> srh) -> {
					MeshNode parentNode = rh.result();
					MeshNode node = srh.result();
					parentNode.getChildren().add(node);
					node = nodeService.save(node);
				}, trh -> {
					MeshNode node = rh.result();
					rc.response().setStatusCode(200).end(toJson(nodeService.transformToRest(rc, node)));
				});

			});
		});

		// TODO fix error handling. This does not fail when tagUuid could not be found
		Route deleteRoute = route("/:uuid/children/:nodeChildUuid").method(DELETE).produces(APPLICATION_JSON);
		deleteRoute.handler(rc -> {
			String projectName = rcs.getProjectName(rc);
			rcs.loadObject(rc, "uuid", projectName, PermissionType.UPDATE, (AsyncResult<MeshNode> rh) -> {
				rcs.loadObject(rc, "nodeChildUuid", projectName, PermissionType.READ, (AsyncResult<MeshNode> srh) -> {
					MeshNode parentNode = rh.result();
					MeshNode node = srh.result();
					parentNode.getChildren().remove(node);
					node = nodeService.save(node);
				}, trh -> {
					MeshNode node = rh.result();
					rc.response().setStatusCode(200).end(toJson(nodeService.transformToRest(rc, node)));
				});
			});

		});

	}

	// TODO filtering, sorting
	private void addChildrenHandler() {
		Route getRoute = route("/:uuid/tags").method(GET).produces(APPLICATION_JSON);
		getRoute.handler(rc -> {
			tagListHandler.handle(rc, (projectName, node, languageTags, pagingInfo) -> {
				return tagService.findTags(rc, projectName, node, languageTags, pagingInfo);
			});
		});

		Route postRoute = route("/:uuid/tags/:tagUuid").method(POST).produces(APPLICATION_JSON);
		postRoute.handler(rc -> {
			String projectName = rcs.getProjectName(rc);
			rcs.loadObject(rc, "uuid", projectName, PermissionType.UPDATE, (AsyncResult<MeshNode> rh) -> {
				rcs.loadObject(rc, "tagUuid", projectName, PermissionType.READ, (AsyncResult<Tag> srh) -> {
					MeshNode node = rh.result();
					Tag tag = srh.result();
					node.addTag(tag);
					node = nodeService.save(node);
				}, trh -> {
					MeshNode node = rh.result();
					rc.response().setStatusCode(200).end(toJson(nodeService.transformToRest(rc, node)));
				});

			});
		});

		// TODO fix error handling. This does not fail when tagUuid could not be found
		Route deleteRoute = route("/:uuid/tags/:tagUuid").method(DELETE).produces(APPLICATION_JSON);
		deleteRoute.handler(rc -> {
			String projectName = rcs.getProjectName(rc);
			rcs.loadObject(rc, "uuid", projectName, PermissionType.UPDATE, (AsyncResult<MeshNode> rh) -> {
				rcs.loadObject(rc, "tagUuid", projectName, PermissionType.READ, (AsyncResult<Tag> srh) -> {
					MeshNode node = rh.result();
					Tag tag = srh.result();
					node.removeTag(tag);
					tag = tagService.save(tag);
				}, trh -> {
					MeshNode node = rh.result();
					rc.response().setStatusCode(200).end(toJson(nodeService.transformToRest(rc, node)));
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
			NodeCreateRequest requestModel = fromJson(rc, NodeCreateRequest.class);

			if (StringUtils.isEmpty(requestModel.getParentNodeUuid())) {
				rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "node_missing_parentnode_field")));
				return;
			}

			Future<MeshNode> contentCreated = Future.future();

			rcs.loadObjectByUuid(rc, requestModel.getParentNodeUuid(), projectName, PermissionType.CREATE, (AsyncResult<MeshNode> rh) -> {

				MeshNode rootNodeForContent = rh.result();
				MeshNode node = new MeshNode();

				if (requestModel.getSchema() == null || StringUtils.isEmpty(requestModel.getSchema().getSchemaName())) {
					rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "error_schema_parameter_missing")));
					return;
				} else {

					ObjectSchema schema = objectSchemaRepository.findByName(requestModel.getSchema().getSchemaName());
					if (schema == null) {
						rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "schema_not_found", requestModel.getSchema().getSchemaName())));
						return;
					} else {
						node.setSchema(schema);
					}
				}

				User user = userService.findUser(rc);
				node.setCreator(user);

				Project project = projectService.findByName(projectName);
				node.addProject(project);

				node = nodeService.save(node);

				/* Add the i18n properties to the newly created tag */
				for (String languageTag : requestModel.getProperties().keySet()) {
					Map<String, String> i18nProperties = requestModel.getProperties(languageTag);
					Language language = languageService.findByLanguageTag(languageTag);
					if (language == null) {
						rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "error_language_not_found", languageTag)));
						return;
					}
					I18NProperties tagProps = new I18NProperties(language);
					for (Map.Entry<String, String> entry : i18nProperties.entrySet()) {
						tagProps.setProperty(entry.getKey(), entry.getValue());
					}
					tagProps = neo4jTemplate.save(tagProps);
					// Create the relationship to the i18n properties
					Translated translated = new Translated(node, tagProps, language);
					translated = neo4jTemplate.save(translated);
					node.getI18nTranslations().add(translated);
				}

				roleService.addCRUDPermissionOnRole(rc, new MeshPermission(rootNodeForContent, PermissionType.CREATE), node);

				node = nodeService.save(node);

				/* Assign the content to the tag and save the tag */
				//				rootTagForContent.(content);
					rootNodeForContent = nodeService.save(rootNodeForContent);

					contentCreated.complete(node);
				}, trh -> {
					MeshNode content = contentCreated.result();
					rc.response().setStatusCode(200).end(toJson(nodeService.transformToRest(rc, content)));
				});

		});
	}

	// TODO filter by project name
	// TODO filtering
	private void addReadHandler() {
		Route route = route("/:uuid").method(GET).produces(APPLICATION_JSON);
		route.handler(rc -> {
			String projectName = rcs.getProjectName(rc);
			rcs.loadObject(rc, "uuid", projectName, PermissionType.READ, (AsyncResult<MeshNode> rh) -> {
			}, trh -> {
				MeshNode node = trh.result();
				rc.response().setStatusCode(200).end(toJson(nodeService.transformToRest(rc, node)));
			});

		});

		Route readAllRoute = route("/").method(GET).produces(APPLICATION_JSON);
		readAllRoute.handler(rc -> {
			String projectName = rcs.getProjectName(rc);
			List<String> languageTags = rcs.getSelectedLanguageTags(rc);
			PagingInfo pagingInfo = rcs.getPagingInfo(rc);

			vertx.executeBlocking((Future<NodeListResponse> bch) -> {
				NodeListResponse listResponse = new NodeListResponse();
				try (Transaction tx = graphDb.beginTx()) {
					Page<MeshNode> contentPage = nodeService.findAll(rc, projectName, languageTags, pagingInfo);
					for (MeshNode content : contentPage) {
						listResponse.getData().add(nodeService.transformToRest(rc, content));
					}
					RestModelPagingHelper.setPaging(listResponse, contentPage, pagingInfo);
					bch.complete(listResponse);
					tx.success();
				}
			}, arh -> {
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
			rcs.loadObject(rc, "uuid", projectName, PermissionType.DELETE, (AsyncResult<MeshNode> rh) -> {
				MeshNode content = rh.result();
				nodeService.delete(content);
			}, trh -> {
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

			rcs.loadObject(rc, "uuid", projectName, PermissionType.READ, (AsyncResult<MeshNode> rh) -> {
				MeshNode content = rh.result();

				NodeUpdateRequest request = fromJson(rc, NodeUpdateRequest.class);
				// Iterate through all properties and update the changed
				// ones
					for (String languageTag : request.getProperties().keySet()) {
						Language language = languageService.findByLanguageTag(languageTag);
						if (language != null) {
							languageTags.add(languageTag);
							Map<String, String> properties = request.getProperties(languageTag);
							if (properties != null) {
								I18NProperties i18nProperties = nodeService.getI18NProperties(content, language);
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
								neo4jTemplate.save(i18nProperties);

							}
						} else {
							rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "error_language_not_found", languageTag)));
							return;
						}

					}
				}, trh -> {
					MeshNode content = trh.result();
					rc.response().setStatusCode(200).end(toJson(nodeService.transformToRest(rc, content)));
				});

		});
	}

}
