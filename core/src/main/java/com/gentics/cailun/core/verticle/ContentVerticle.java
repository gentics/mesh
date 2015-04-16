package com.gentics.cailun.core.verticle;

import static com.gentics.cailun.util.JsonUtils.fromJson;
import static com.gentics.cailun.util.JsonUtils.toJson;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import io.vertx.ext.apex.Route;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import com.gentics.cailun.error.EntityNotFoundException;
import com.gentics.cailun.error.HttpStatusCodeErrorException;
import com.gentics.cailun.path.PagingInfo;
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

			Content content = new Content();
			try (Transaction tx = graphDb.beginTx()) {

				if (StringUtils.isEmpty(requestModel.getTagUuid())) {
					throw new HttpStatusCodeErrorException(400, i18n.get(rc, "content_missing_parenttag_field"));
				}

				Tag rootTagForContent = tagService.findByUUID(projectName, requestModel.getTagUuid());
				if (rootTagForContent == null) {
					// TODO i18n
					String message = "Root tag could not be found. Maybe it is not part of project {" + projectName + "}";
					throw new HttpStatusCodeErrorException(400, message);
				}
				failOnMissingPermission(rc, rootTagForContent, PermissionType.CREATE);

				if (StringUtils.isEmpty(requestModel.getSchemaName())) {
					// TODO i18n
					throw new HttpStatusCodeErrorException(400, "No valid schema name was specified.");
				} else {
					// TODO load the schema and set the reference to the tag
					ObjectSchema schema = objectSchemaRepository.findByName(requestModel.getSchemaName());
					if (schema == null) {
						// TODO i18n
						throw new HttpStatusCodeErrorException(400, "The schema \"" + requestModel.getSchemaName() + "\" could not be found.");
					} else {
						content.setSchema(schema);
					}
				}

				User user = springConfiguration.authService().getUser(rc);
				content.setCreator(user);

				// TODO maybe projects should not be a set?
				Project project = projectService.findByName(projectName);
				content.addProject(project);
				content = neo4jTemplate.save(content);

				// Add the i18n properties to the newly created tag
				for (String languageTag : requestModel.getProperties().keySet()) {
					Map<String, String> i18nProperties = requestModel.getProperties(languageTag);
					Language language = languageService.findByLanguageTag(languageTag);
					if (language == null) {
						throw new HttpStatusCodeErrorException(400, i18n.get(rc, "error_language_not_found", languageTag));
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
				tx.success();
			}

			try (Transaction tx = graphDb.beginTx()) {
				// Reload in order to update uuid field
				content = contentService.reload(content);
				rc.response().setStatusCode(200);
				rc.response().end(toJson(contentService.transformToRest(rc, content, languageTags, 0)));
			}

			// // TODO simplify language handling - looks a bit chaotic
			// // Language language = languageService.findByLanguageTag(requestModel.getLanguageTag());
			// handleResponse(rc, content, Arrays.asList(language.getName()));
			// } else {
			// // TODO handle error, i18n
			// throw new HttpStatusCodeErrorException(500, "Could not save content");
			// }

		});
	}

	private void addReadHandler() {

		Route route = route("/:uuid").method(GET);
		route.handler(rc -> {
			String projectName = getProjectName(rc);
			int depth = getDepth(rc);
			Content content = null;
			try (Transaction tx = graphDb.beginTx()) {
				content = getObject(rc, "uuid", PermissionType.READ);
				List<String> languageTags = getSelectedLanguageTags(rc);
				rc.response().setStatusCode(200);
				rc.response().end(toJson(contentService.transformToRest(rc, content, languageTags, depth)));
				tx.success();
			}

		});

		Route readAllRoute = route("/").method(GET).produces(APPLICATION_JSON);
		readAllRoute.handler(rc -> {
			String projectName = getProjectName(rc);
			List<String> languageTags = getSelectedLanguageTags(rc);

			// TODO filtering
				ContentListResponse listResponse = new ContentListResponse();
				try (Transaction tx = graphDb.beginTx()) {
					PagingInfo pagingInfo = getPagingInfo(rc);
					User requestUser = springConfiguration.authService().getUser(rc);

					Page<Content> contentPage = contentService.findAllVisible(requestUser, projectName, languageTags, pagingInfo);
					for (Content content : contentPage) {
						listResponse.getData().add(contentService.transformToRest(rc, content, languageTags, 0));
					}
					RestModelPagingHelper.setPaging(listResponse, contentPage.getNumber(), contentPage.getTotalPages(), pagingInfo.getPerPage(),
							contentPage.getTotalElements());
					tx.success();
				}
				rc.response().setStatusCode(200);
				rc.response().end(toJson(listResponse));
			});
	}

	private void addDeleteHandler() {
		Route route = route("/:uuid").method(DELETE);
		route.handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			String projectName = getProjectName(rc);

			Content content = contentService.findByUUID(projectName, uuid);
			if (content == null) {
				throw new EntityNotFoundException(i18n.get(rc, "content_not_found_for_uuid", uuid));
			}
			failOnMissingPermission(rc, content, PermissionType.DELETE);

			contentService.delete(content);
			rc.response().setStatusCode(200);
			rc.response().end(toJson(new GenericMessageResponse(i18n.get(rc, "content_deleted", uuid))));
		});
	}

	private void addUpdateHandler() {

		Route route = route("/:uuid").consumes(APPLICATION_JSON).method(PUT);
		route.handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			String projectName = getProjectName(rc);
			List<String> languageTags = getSelectedLanguageTags(rc);
			Content content;
			try (Transaction tx = graphDb.beginTx()) {
				content = getObject(rc, "uuid", PermissionType.READ);
				// TODO update other fields as well?
				// TODO Update user information
				ContentUpdateRequest request = fromJson(rc, ContentUpdateRequest.class);
				// Iterate through all properties and update the changed ones
				for (String languageTag : request.getProperties().keySet()) {
					Language language = languageService.findByLanguageTag(languageTag);
					if (language != null) {
						languageTags.add(languageTag);
						Map<String, String> properties = request.getProperties(languageTag);
						if (properties != null) {
							// TODO use schema and only handle those i18n properties that were specified within the schema.
							I18NProperties i18nProperties = contentService.getI18NProperties(content, language);
							for (Map.Entry<String, String> set : properties.entrySet()) {
								String key = set.getKey();
								String value = set.getValue();
								String i18nValue = i18nProperties.getProperty(key);
								// Tag does not have the value so lets create it
								if (i18nValue == null) {
									i18nProperties.setProperty(key, value);
								} else {
									// Lets compare and update if the value has changed
									if (!value.equals(i18nValue)) {
										i18nProperties.setProperty(key, value);
									}
								}
							}
							neo4jTemplate.save(i18nProperties);

							// // Check whether there are any key missing in the request.
							// // This would mean we should remove those i18n properties. First lets collect those
							// // keys
							// Set<String> keysToBeRemoved = new HashSet<>();
							// for (String i18nKey : i18nProperties.getProperties().getPropertyKeys()) {
							// if (!properties.containsKey(i18nKey)) {
							// keysToBeRemoved.add(i18nKey);
							// }
							// }
							//
							// // Now remove the keys
							// for (String key : keysToBeRemoved) {
							// i18nProperties.removeProperty(key);
							// }

						}
					} else {
						throw new HttpStatusCodeErrorException(400, i18n.get(rc, "error_language_not_found", languageTag));
					}

				}
				tx.success();
			}
			// TODO check for content==null?
			try (Transaction tx = graphDb.beginTx()) {
				rc.response().setStatusCode(200);
				rc.response().end(toJson(contentService.transformToRest(rc, content, languageTags, 0)));
				tx.success();
			}

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
