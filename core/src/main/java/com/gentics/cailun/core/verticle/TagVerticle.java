package com.gentics.cailun.core.verticle;

import static com.gentics.cailun.util.JsonUtils.toJson;
import static com.gentics.cailun.util.UUIDUtil.isUUID;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import io.vertx.ext.apex.core.Route;
import io.vertx.ext.apex.core.RoutingContext;

import java.util.List;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.AbstractProjectRestVerticle;
import com.gentics.cailun.core.data.model.Tag;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.service.TagService;
import com.gentics.cailun.core.rest.common.response.GenericMessageResponse;
import com.gentics.cailun.error.EntityNotFoundException;
import com.gentics.cailun.error.HttpStatusCodeErrorException;
import com.gentics.cailun.path.Path;

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

	@Autowired
	private TagService tagService;

	@Autowired
	private Neo4jTemplate template;

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

	private void addPathHandler() {
		Route route = getRouter().routeWithRegex("\\/(.*)");

		// TODO add .produces(APPLICATION_JSON)
		route.method(PUT).handler(rc -> {
			String path = rc.request().params().get("param0");
			if (isUUID(path)) {
				uuidPutHandler(rc);
			} else {
				pathPutHandler(rc);
			}
		});

		// TODO add produces(APPLICATION_JSON).
		route.method(DELETE).handler(rc -> {
			String path = rc.request().params().get("param0");
			if (isUUID(path)) {
				uuidDeleteHandler(rc);
			} else {
				pathDeleteHandler(rc);
			}
		});

		// TODO add produces(APPLICATION_JSON).
		route.method(POST).handler(rc -> {
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
		route.method(GET).handler(rc -> {
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
		throw new HttpStatusCodeErrorException(501, "Not implemented");
	}

	private void pathDeleteHandler(RoutingContext rc) {
		throw new HttpStatusCodeErrorException(501, "Not implemented");
	}

	private void pathGetHandler(RoutingContext rc) {
		String projectName = getProjectName(rc);
		String path = rc.request().params().get("param0");
		List<String> languages = getSelectedLanguages(rc);

		Path tagPath = tagService.findByProjectPath(projectName, path);
		if (tagPath.getLast() != null) {

			Tag tag = tagService.projectTo(tagPath.getLast().getNode(), Tag.class);
			if (tag == null) {
				String message = i18n.get(rc, "tag_not_found_for_path", path);
				throw new EntityNotFoundException(message);
			}
			languages.add(tagPath.getLast().getLanguageTag());

			failOnMissingPermission(rc, tag, PermissionType.READ);
			rc.response().end(toJson(tagService.transformToRest(tag, languages)));
			return;
		} else {
			String message = i18n.get(rc, "tag_not_found_for_path", path);
			throw new EntityNotFoundException(message);
		}
	}

	private void uuidGetHandler(RoutingContext rc) {
		String uuid = rc.request().params().get("param0");
		String projectName = getProjectName(rc);
		List<String> languages = getSelectedLanguages(rc);

		Tag tag = tagService.findByUUID(projectName, uuid);
		if (tag != null) {
			failOnMissingPermission(rc, tag, PermissionType.READ);
			rc.response().end(toJson(tagService.transformToRest(tag, languages)));
		} else {
			String message = i18n.get(rc, "tag_not_found");
			throw new EntityNotFoundException(message);
		}
	}

	private void uuidDeleteHandler(RoutingContext rc) {
		throw new HttpStatusCodeErrorException(501, "Not implemented");
	}

	private void uuidPutHandler(RoutingContext rc) {
		throw new HttpStatusCodeErrorException(501, "Not implemented");
	}

}
