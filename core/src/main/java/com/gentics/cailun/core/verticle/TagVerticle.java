package com.gentics.cailun.core.verticle;

import static com.gentics.cailun.util.JsonUtils.fromJson;
import static com.gentics.cailun.util.JsonUtils.toJson;
import static com.gentics.cailun.util.UUIDUtil.isUUID;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.apex.core.Route;
import io.vertx.ext.apex.core.RoutingContext;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.AbstractProjectRestVerticle;
import com.gentics.cailun.core.data.model.I18NProperties;
import com.gentics.cailun.core.data.model.Language;
import com.gentics.cailun.core.data.model.Tag;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.service.LanguageService;
import com.gentics.cailun.core.data.service.TagService;
import com.gentics.cailun.core.rest.common.response.GenericMessageResponse;
import com.gentics.cailun.core.rest.tag.request.TagUpdateRequest;
import com.gentics.cailun.error.EntityNotFoundException;
import com.gentics.cailun.error.HttpStatusCodeErrorException;
import com.gentics.cailun.path.Path;
import com.gentics.cailun.path.PathSegment;

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
	}

	private void addCRUDHandlers() {
		addPathHandler();
	}

	private Route pathRoute() {
		return getRouter().routeWithRegex("\\/(.*)");
	}

	private void addPathHandler() {

		// TODO add .produces(APPLICATION_JSON)
		pathRoute().method(PUT).handler(rc -> {
			String path = rc.request().params().get("param0");
			if (isUUID(path)) {
				uuidPutHandler(rc);
			} else {
				pathPutHandler(rc);
			}
		});

		// TODO add produces(APPLICATION_JSON).
		pathRoute().method(DELETE).handler(rc -> {
			String path = rc.request().params().get("param0");
			if (isUUID(path)) {
				uuidDeleteHandler(rc);
			} else {
				pathDeleteHandler(rc);
			}
		});

		// TODO add produces(APPLICATION_JSON).
		pathRoute().method(POST).handler(rc -> {
			String path = rc.request().params().get("param0");
			if (isUUID(path)) {
				String msg = "";
				// TODO unify this error
				rc.response().setStatusCode(500);
				rc.response().end(toJson(new GenericMessageResponse(msg)));
				return;
			} else {
				pathPostHandler(rc);
			}
		});

		// TODO add .produces(APPLICATION_JSON)
		pathRoute().method(GET).handler(rc -> {
			String path = rc.request().params().get("param0");
			if (isUUID(path)) {
				uuidGetHandler(rc);
			} else {
				pathGetHandler(rc);
			}
		});

	}

	private void pathPostHandler(RoutingContext rc) {
		throw new HttpStatusCodeErrorException(501, "Not implemented");
	}

	private void pathPutHandler(RoutingContext rc) {
		String projectName = getProjectName(rc);
		String path = rc.request().params().get("param0");
		List<String> languages = getSelectedLanguageTags(rc);

		Path tagPath = tagService.findByProjectPath(projectName, path);
		PathSegment lastSegment = tagPath.getLast();
		if (lastSegment != null) {
			Tag tag = tagService.projectTo(lastSegment.getNode(), Tag.class);
			if (tag == null) {
				String message = i18n.get(rc, "tag_not_found_for_path", path);
				throw new EntityNotFoundException(message);
			}
			failOnMissingPermission(rc, tag, PermissionType.UPDATE);
			languages.add(lastSegment.getLanguageTag());

			// TODO handle update

			rc.response().end(toJson(tagService.transformToRest(tag, languages)));
			return;
		} else {
			throw new EntityNotFoundException(i18n.get(rc, "tag_not_found_for_path", path));
		}
	}

	private void pathDeleteHandler(RoutingContext rc) {
		throw new HttpStatusCodeErrorException(501, "Not implemented");
	}

	private void pathGetHandler(RoutingContext rc) {
		String projectName = getProjectName(rc);
		String path = rc.request().params().get("param0");
		List<String> languages = getSelectedLanguageTags(rc);

		Path tagPath = tagService.findByProjectPath(projectName, path);
		PathSegment lastSegment = tagPath.getLast();
		if (lastSegment != null) {

			Tag tag = tagService.projectTo(lastSegment.getNode(), Tag.class);
			if (tag == null) {
				String message = i18n.get(rc, "tag_not_found_for_path", path);
				throw new EntityNotFoundException(message);
			}
			failOnMissingPermission(rc, tag, PermissionType.READ);
			languages.add(lastSegment.getLanguageTag());

			rc.response().end(toJson(tagService.transformToRest(tag, languages)));
			return;
		} else {
			throw new EntityNotFoundException(i18n.get(rc, "tag_not_found_for_path", path));
		}
	}

	private void uuidGetHandler(RoutingContext rc) {
		String uuid = rc.request().params().get("param0");
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
	}

	private void uuidDeleteHandler(RoutingContext rc) {
		throw new HttpStatusCodeErrorException(501, "Not implemented");
	}

	private void uuidPutHandler(RoutingContext rc) {
		String projectName = getProjectName(rc);
		String uuid = rc.request().params().get("param0");
		List<String> languageTags = getSelectedLanguageTags(rc);

		Tag tag = tagService.findByUUID(projectName, uuid);
		if (tag == null) {
			String message = i18n.get(rc, "tag_not_found", uuid);
			throw new EntityNotFoundException(message);
		}
		failOnMissingPermission(rc, tag, PermissionType.UPDATE);

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

		rc.response().end(toJson(tagService.transformToRest(tag, languageTags)));
		return;
	}
}
