package com.gentics.cailun.core.verticle;

import static com.gentics.cailun.util.JsonUtils.fromJson;
import static com.gentics.cailun.util.JsonUtils.toJson;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.apex.core.Route;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jacpfx.vertx.spring.SpringVerticle;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.AbstractProjectRestVerticle;
import com.gentics.cailun.core.data.model.I18NProperties;
import com.gentics.cailun.core.data.model.Language;
import com.gentics.cailun.core.data.model.Project;
import com.gentics.cailun.core.data.model.Tag;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.model.relationship.Translated;
import com.gentics.cailun.core.data.service.LanguageService;
import com.gentics.cailun.core.data.service.TagService;
import com.gentics.cailun.core.rest.common.response.GenericMessageResponse;
import com.gentics.cailun.core.rest.tag.request.TagCreateRequest;
import com.gentics.cailun.core.rest.tag.request.TagUpdateRequest;
import com.gentics.cailun.error.EntityNotFoundException;
import com.gentics.cailun.error.HttpStatusCodeErrorException;


/**
 * The tag verticle provides rest endpoints which allow manipulation and handling of tag related objects.
 * 
 * @author johannes2
 *
 */
@Component
@Scope("singleton")
@SpringVerticle
public class TagVerticle extends AbstractProjectRestVerticle {

	private static final Logger log = LoggerFactory.getLogger(TagVerticle.class);

	@Autowired
	private TagService tagService;

	@Autowired
	private Neo4jTemplate template;

	@Autowired
	LanguageService languageService;

	public TagVerticle() {
		super("tags");
	}

	@Override
	public void registerEndPoints() throws Exception {
		addCRUDHandlers();

		addTagFilesHandlers();
		addTagSubTagHandlers();
	}

	private void addCRUDHandlers() {
		addCreateHandler();
		addReadHandler();
		addUpdateHandler();
		addDeleteHandler();
	}

	private void addUpdateHandler() {
		Route route = route("/:uuid").method(PUT);
		route.handler(rc -> {
			String projectName = getProjectName(rc);
			String uuid = rc.request().params().get("uuid");
			Tag tag = tagService.findByUUID(projectName, uuid);
			if (tag == null) {
				throw new EntityNotFoundException(i18n.get(rc, "tag_not_found", uuid));
			}
			failOnMissingPermission(rc, tag, PermissionType.UPDATE);
			List<String> languageTags = getSelectedLanguageTags(rc);
			try (Transaction tx = graphDb.beginTx()) {
				// TODO update other fields as well?
				// TODO Update user information
				TagUpdateRequest requestModel = fromJson(rc, TagUpdateRequest.class);
				// Iterate through all properties and update the changed ones
				for (String languageTag : languageTags) {
					Language language = languageService.findByLanguageTag(languageTag);
					if (language != null) {
						Map<String, String> properties = requestModel.getProperties(languageTag);
						if (properties != null) {
							// TODO use schema and only handle those i18n properties that were specified within the schema.
							I18NProperties i18nProperties = tag.getI18NProperties(language);
							for (Map.Entry<String, String> set : properties.entrySet()) {
								String key = set.getKey();
								String value = set.getValue();
								String i18nValue = i18nProperties.getProperty(key);
								// Tag does not have the value so lets create it
								if (i18nValue == null) {
									i18nProperties.addProperty(key, value);
								} else {
									// Lets compare and update if the value has changed
									if (!value.equals(i18nValue)) {
										i18nProperties.addProperty(key, value);
									}
								}
							}

							// Check whether there are any key missing in the request.
							// This would mean we should remove those i18n properties. First lets collect those
							// keys
							Set<String> keysToBeRemoved = new HashSet<>();
							for (String i18nKey : i18nProperties.getProperties().getPropertyKeys()) {
								if (!properties.containsKey(i18nKey)) {
									keysToBeRemoved.add(i18nKey);
								}
							}

							// Now remove the keys
							for (String key : keysToBeRemoved) {
								i18nProperties.removeProperty(key);
							}

						}
					} else {
						log.error("Could not find language for languageTag {" + languageTag + "}");
					}

				}
				tx.success();
			}
		});

	}

	private void addCreateHandler() {
		// TODO Add produces and consums
		Route route = route("/").method(POST);
		route.handler(rc -> {
			String projectName = getProjectName(rc);
			TagCreateRequest request = fromJson(rc, TagCreateRequest.class);
			List<String> languageTags = getSelectedLanguageTags(rc);

			Tag rootTag = tagService.findByUUID(projectName, request.getTagUuid());
			failOnMissingPermission(rc, rootTag, PermissionType.CREATE);

			Tag newTag = new Tag();
			newTag.setSchema(request.getSchemaName());
			//TODO maybe projects should not be a set?
			Project project = projectService.findByName(projectName);
			newTag.addProject(project);
			// TODO handle creator

			// Add the i18n properties to the newly created tag
			for (String languageTag : request.getProperties().keySet()) {
				Map<String, String> i18nProperties = request.getProperties(languageTag);
				Language language = languageService.findByLanguageTag(languageTag);
				I18NProperties tagProps = new I18NProperties(language);
				for (Map.Entry<String, String> entry : i18nProperties.entrySet()) {
					tagProps.addProperty(entry.getKey(), entry.getValue());
				}
				// Create the relationship to the i18n properties
				Translated translated = new Translated(newTag, tagProps, language);
				newTag.getI18nTranslations().add(translated);
			}
			newTag = tagService.save(newTag);
			newTag = tagService.reload(newTag);

			rc.response().setStatusCode(200);
			rc.response().end(toJson(tagService.transformToRest(newTag, languageTags)));

		});
	}

	private void addReadHandler() {
		Route readAllRoute = route("/").method(GET);
		readAllRoute.handler(rc -> {
			String projectName = getProjectName(rc);
			//TODO paging, filtering
			Iterable<Tag> allTags = tagService.findAll(projectName);
		});

		Route route = route("/:uuid").method(GET);
		route.handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			String projectName = getProjectName(rc);
			List<String> languages = getSelectedLanguageTags(rc);

			Tag tag = tagService.findByUUID(projectName, uuid);
			if (tag != null) {
				failOnMissingPermission(rc, tag, PermissionType.READ);
				rc.response().end(toJson(tagService.transformToRest(tag, languages)));
			} else {
				String message = i18n.get(rc, "tag_not_found", uuid);
				throw new EntityNotFoundException(message);
			}
		});
	}

	private void addDeleteHandler() {
		Route route = route("/:uuid").method(DELETE);
		route.handler(rc -> {
			String projectName = getProjectName(rc);
			String uuid = rc.request().params().get("uuid");
			Tag tag = tagService.findByUUID(projectName, uuid);
			if (tag == null) {
				throw new EntityNotFoundException(i18n.get(rc, "tag_not_found", uuid));
			}

			failOnMissingPermission(rc, tag, PermissionType.DELETE);
			tagService.delete(tag);
			rc.response().setStatusCode(200);
			// TODO i18n
			rc.response().end(toJson(new GenericMessageResponse("Deleted tag")));
		});
	}

	private void addTagSubTagHandlers() {
		Route postRoute = route("/:tag_uuid/tags/:subtag_uuid").method(POST);
		postRoute.handler(rc -> {
			String tagUuid = rc.request().params().get("tag_uuid");
			String subTagUuid = rc.request().params().get("subtag_uuid");
			if (StringUtils.isEmpty(tagUuid) || StringUtils.isEmpty(subTagUuid)) {
				throw new HttpStatusCodeErrorException(404, "Missing uuid parameter");
			}
			String projectName = getProjectName(rc);

			// Load objects and check permissions etc.
				Tag tag = tagService.findByUUID(projectName, tagUuid);
				if (tag == null) {
					throw new EntityNotFoundException(i18n.get(rc, "tag_not_found", tagUuid));
				}
				failOnMissingPermission(rc, tag, PermissionType.UPDATE);
				Tag subTag = tagService.findByUUID(projectName, subTagUuid);
				if (subTag == null) {
					throw new EntityNotFoundException(i18n.get(rc, "tag_not_found", subTagUuid));
				}
				failOnMissingPermission(rc, subTag, PermissionType.READ);

				List<String> languageTags = getSelectedLanguageTags(rc);
				tag.addTag(subTag);
				tag = tagService.save(tag);

				rc.response().setStatusCode(200);
				rc.response().end(toJson(tagService.transformToRest(tag, languageTags)));

			});

		Route deleteRoute = route("/:tag_uuid/tags/:subtag_uuid").method(DELETE);
		deleteRoute.handler(rc -> {
			String tagUuid = rc.request().params().get("tag_uuid");
			String subTagUuid = rc.request().params().get("subtag_uuid");
			if (StringUtils.isEmpty(tagUuid) || StringUtils.isEmpty(subTagUuid)) {
				throw new HttpStatusCodeErrorException(404, "Missing uuid parameter");
			}
			String projectName = getProjectName(rc);

			// Load objects and check permissions etc.
				Tag tag = tagService.findByUUID(projectName, tagUuid);
				if (tag == null) {
					throw new EntityNotFoundException(i18n.get(rc, "tag_not_found", tagUuid));
				}
				failOnMissingPermission(rc, tag, PermissionType.UPDATE);
				Tag subTag = tagService.findByUUID(projectName, subTagUuid);
				if (subTag == null) {
					throw new EntityNotFoundException(i18n.get(rc, "tag_not_found", subTagUuid));
				}
				failOnMissingPermission(rc, subTag, PermissionType.READ);

				List<String> languageTags = getSelectedLanguageTags(rc);
				tag.removeTag(subTag);
				tag = tagService.save(tag);

				rc.response().setStatusCode(200);
				rc.response().end(toJson(tagService.transformToRest(tag, languageTags)));

			});

	}

	private void addTagFilesHandlers() {
		Route postRoute = route("/:tag_uuid/files/:file_uuid").method(POST);
		postRoute.handler(rc -> {
			String tagUuid = rc.request().params().get("tag_uuid");
			String fileUuid = rc.request().params().get("file_uuid");
			if (StringUtils.isEmpty(tagUuid) || StringUtils.isEmpty(fileUuid)) {
				throw new HttpStatusCodeErrorException(404, "Missing uuid parameter");
			}
			String projectName = getProjectName(rc);

			// Load objects and check permissions etc.
				Tag tag = tagService.findByUUID(projectName, tagUuid);
				if (tag == null) {
					throw new EntityNotFoundException(i18n.get(rc, "tag_not_found", tagUuid));
				}
				failOnMissingPermission(rc, tag, PermissionType.UPDATE);
				// File file = file
				// TODO impl
			});

		Route deleteRoute = route("/:tag_uuid/files/:file_uuid").method(DELETE);
		deleteRoute.handler(rc -> {
			String tagUuid = rc.request().params().get("tag_uuid");
			String fileUuid = rc.request().params().get("file_uuid");
			if (StringUtils.isEmpty(tagUuid) || StringUtils.isEmpty(fileUuid)) {
				throw new HttpStatusCodeErrorException(404, "Missing uuid parameter");
			}
			String projectName = getProjectName(rc);

			// Load objects and check permissions etc.
				Tag tag = tagService.findByUUID(projectName, tagUuid);
				if (tag == null) {
					throw new EntityNotFoundException(i18n.get(rc, "tag_not_found", tagUuid));
				}
				failOnMissingPermission(rc, tag, PermissionType.UPDATE);
				// File file = file

				// TODO impl
			});

	}

}
