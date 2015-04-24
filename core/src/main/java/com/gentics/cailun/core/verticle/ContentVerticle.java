package com.gentics.cailun.core.verticle;

import static com.gentics.cailun.util.JsonUtils.fromJson;
import static com.gentics.cailun.util.JsonUtils.toJson;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.ext.apex.Route;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.StringUtils;
import org.jacpfx.vertx.spring.SpringVerticle;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.AbstractProjectRestVerticle;
import com.gentics.cailun.core.data.model.Content;
import com.gentics.cailun.core.data.model.I18NProperties;
import com.gentics.cailun.core.data.model.Language;
import com.gentics.cailun.core.data.model.ObjectSchema;
import com.gentics.cailun.core.data.model.Project;
import com.gentics.cailun.core.data.model.Tag;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.model.relationship.Translated;
import com.gentics.cailun.core.link.CaiLunLinkResolver;
import com.gentics.cailun.core.link.CaiLunLinkResolverFactoryImpl;
import com.gentics.cailun.core.link.LinkReplacer;
import com.gentics.cailun.core.repository.ObjectSchemaRepository;
import com.gentics.cailun.core.rest.common.response.GenericMessageResponse;
import com.gentics.cailun.core.rest.content.request.ContentCreateRequest;
import com.gentics.cailun.core.rest.content.request.ContentUpdateRequest;
import com.gentics.cailun.core.rest.content.response.ContentListResponse;
import com.gentics.cailun.error.HttpStatusCodeErrorException;
import com.gentics.cailun.paging.PagingInfo;
import com.gentics.cailun.util.RestModelPagingHelper;

/**
 * The content verticle adds rest endpoints for manipulating contents.
 */
@Component
@Scope("singleton")
@SpringVerticle
public class ContentVerticle extends AbstractProjectRestVerticle {

	private static final Logger log = LoggerFactory.getLogger(ContentVerticle.class);

	@Autowired
	private CaiLunLinkResolverFactoryImpl<CaiLunLinkResolver> resolver;

	@Autowired
	private ObjectSchemaRepository objectSchemaRepository;

	public ContentVerticle() {
		super("contents");
	}

	@Override
	public void registerEndPoints() throws Exception {
		route("/*").handler(springConfiguration.authHandler());
		addCreateHandler();
		addReadHandler();
		addUpdateHandler();
		addDeleteHandler();
	}

	private void addCreateHandler() {
		Route route = route("/").method(POST);
		route.handler(rc -> {
			String projectName = getProjectName(rc);
			ContentCreateRequest requestModel = fromJson(rc, ContentCreateRequest.class);
			List<String> languageTags = getSelectedLanguageTags(rc);

			if (StringUtils.isEmpty(requestModel.getTagUuid())) {
				rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "content_missing_parenttag_field")));
				return;
			}

			Future<Content> contentCreated = Future.future();

			loadObjectByUuid(rc, requestModel.getTagUuid(), PermissionType.CREATE, (AsyncResult<Tag> rh) -> {

				Tag rootTagForContent = rh.result();
				Content content = new Content();

				if (requestModel.getSchema() == null || StringUtils.isEmpty(requestModel.getSchema().getSchemaName())) {
					rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "error_schema_parameter_missing")));
					return;
				} else {
					// TODO handle schema by name / by uuid - move that code in a seperate handler
					// TODO load the schema and set the reference to the tag
					ObjectSchema schema = objectSchemaRepository.findByName(requestModel.getSchema().getSchemaName());
					if (schema == null) {
						rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "schema_not_found", requestModel.getSchema().getSchemaName())));
						return;
					} else {
						content.setSchema(schema);
					}
				}

				User user = userService.findUser(rc);
				content.setCreator(user);

				// TODO maybe projects should not be a set?
					Project project = projectService.findByName(projectName);
					content.addProject(project);

					// Add the i18n properties to the newly created tag
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
						Translated translated = new Translated(content, tagProps, language);
						translated = neo4jTemplate.save(translated);
						content.getI18nTranslations().add(translated);
					}

					content = contentService.save(content);

					// Assign the content to the tag and save the tag
					rootTagForContent.addContent(content);
					rootTagForContent = tagService.save(rootTagForContent);
					contentCreated.complete(content);
				}, trh -> {
					Content content = contentCreated.result();
					rc.response().setStatusCode(200).end(toJson(contentService.transformToRest(rc, content, languageTags, 0)));
				});

		});
	}

	// TODO filter by project name
	// TODO filtering
	private void addReadHandler() {
		Route route = route("/:uuid").method(GET).produces(APPLICATION_JSON);
		route.handler(rc -> {
			String projectName = getProjectName(rc);
			int depth = getDepth(rc);
			List<String> languageTags = getSelectedLanguageTags(rc);

			loadObject(rc, "uuid", PermissionType.READ, (AsyncResult<Content> rh) -> {
			}, trh -> {
				Content content = trh.result();
				rc.response().setStatusCode(200).end(toJson(contentService.transformToRest(rc, content, languageTags, depth)));
			});
		});

		Route readAllRoute = route("/").method(GET).produces(APPLICATION_JSON);
		readAllRoute.handler(rc -> {
			String projectName = getProjectName(rc);
			List<String> languageTags = getSelectedLanguageTags(rc);
			int depth = getDepth(rc);

			vertx.executeBlocking((Future<ContentListResponse> bch) -> {
				ContentListResponse listResponse = new ContentListResponse();
				try (Transaction tx = graphDb.beginTx()) {
					PagingInfo pagingInfo = getPagingInfo(rc);
					User user = userService.findUser(rc);
					Page<Content> contentPage = contentService.findAllVisible(user, projectName, languageTags, pagingInfo);
					for (Content content : contentPage) {
						listResponse.getData().add(contentService.transformToRest(rc, content, languageTags, depth));
					}
					RestModelPagingHelper.setPaging(listResponse, contentPage, pagingInfo);
					bch.complete(listResponse);
					tx.success();
				}
			}, arh -> {
				ContentListResponse listResponse = arh.result();
				rc.response().setStatusCode(200).end(toJson(listResponse));
			});
		});
	}

	// TODO filter project name
	private void addDeleteHandler() {
		Route route = route("/:uuid").method(DELETE).produces(APPLICATION_JSON);
		route.handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			String projectName = getProjectName(rc);

			loadObject(rc, "uuid", PermissionType.DELETE, (AsyncResult<Content> rh) -> {
				Content content = rh.result();
				contentService.delete(content);
			}, trh -> {
				rc.response().setStatusCode(200).end(toJson(new GenericMessageResponse(i18n.get(rc, "content_deleted", uuid))));
			});
		});
	}

	// TODO filter by project name
	// TODO handle depth
	// TODO update other fields as well?
	// TODO Update user information
	// TODO use schema and only handle those i18n properties that were specified within the schema.
	private void addUpdateHandler() {

		Route route = route("/:uuid").method(PUT).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		route.handler(rc -> {
			String projectName = getProjectName(rc);
			List<String> languageTags = getSelectedLanguageTags(rc);

			loadObject(rc, "uuid", PermissionType.READ, (AsyncResult<Content> rh) -> {
				Content content = rh.result();

				ContentUpdateRequest request = fromJson(rc, ContentUpdateRequest.class);
				// Iterate through all properties and update the changed ones
					for (String languageTag : request.getProperties().keySet()) {
						Language language = languageService.findByLanguageTag(languageTag);
						if (language != null) {
							languageTags.add(languageTag);
							Map<String, String> properties = request.getProperties(languageTag);
							if (properties != null) {
								I18NProperties i18nProperties = contentService.getI18NProperties(content, language);
								for (Map.Entry<String, String> set : properties.entrySet()) {
									String key = set.getKey();
									String value = set.getValue();
									String i18nValue = i18nProperties.getProperty(key);
									/* Tag does not have the value so lets create it */
									if (i18nValue == null) {
										i18nProperties.setProperty(key, value);
									} else {
										/* Lets compare and update if the value has changed */
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
					Content content = trh.result();
					rc.response().setStatusCode(200).end(toJson(contentService.transformToRest(rc, content, languageTags, 0)));
				});

		});
	}

	private void resolveLinks(Content content) throws InterruptedException, ExecutionException {
		// TODO fix issues with generics - Maybe move the link replacer to a spring service
		// TODO handle language
		Language language = null;
		LinkReplacer replacer = new LinkReplacer(resolver);
		// content.setContent(language, replacer.replace(content.getContent(language)));
	}

}
