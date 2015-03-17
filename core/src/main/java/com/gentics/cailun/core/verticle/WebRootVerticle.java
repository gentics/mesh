package com.gentics.cailun.core.verticle;

import static com.gentics.cailun.util.JsonUtils.toJson;
import static io.vertx.core.http.HttpMethod.GET;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.apex.core.Route;

import java.util.List;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.AbstractProjectRestVerticle;
import com.gentics.cailun.core.data.model.Tag;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.service.LanguageService;
import com.gentics.cailun.core.data.service.TagService;
import com.gentics.cailun.error.EntityNotFoundException;
import com.gentics.cailun.path.Path;
import com.gentics.cailun.path.PathSegment;

@Component
@Scope("singleton")
@SpringVerticle
public class WebRootVerticle extends AbstractProjectRestVerticle {

	private static final Logger log = LoggerFactory.getLogger(WebRootVerticle.class);

	@Autowired
	private TagService tagService;

	@Autowired
	private Neo4jTemplate template;

	@Autowired
	LanguageService languageService;

	protected WebRootVerticle() {
		super("webroot");
	}

	@Override
	public void registerEndPoints() throws Exception {
		addPathHandler();
	}

	private Route pathRoute() {
		return getRouter().routeWithRegex("\\/(.*)");
	}

	// // try (Transaction tx = springConfig.getGraphDatabaseService().beginTx()) {
	// content = contentService.save(projectName, path, requestModel);
	// if (content != null) {
	// rootTagForContent.addFile(content);
	// tagService.save(rootTagForContent);
	// // tx.success();
	// } else {
	// rc.response().end("error");
	// // tx.failure();
	// }
	// // }
	// if (content != null) {
	// // Reload in order to update uuid field
	// content = contentService.reload(content);
	// // TODO simplify language handling - looks a bit chaotic
	// // Language language = languageService.findByLanguageTag(requestModel.getLanguageTag());
	// Language language = null;
	// // TODO check for npe - or see above
	// handleResponse(rc, content, Arrays.asList(language.getName()));
	// // rc.response().end("jow" + " " + path + " " + projectName);
	// } else {
	// // TODO handle error, i18n
	// throw new HttpStatusCodeErrorException(500, "Could not save content");
	// }

	// route().method(PUT).consumes(APPLICATION_JSON).handler(rc -> {
	// // TODO handle request
	// // rc.response().end(toJson(new GenericResponse<>()));
	// });

	// }
	// private void pathPutHandler(RoutingContext rc) {
	// String projectName = getProjectName(rc);
	// String path = rc.request().params().get("param0");
	// List<String> languages = getSelectedLanguageTags(rc);
	//
	// Path tagPath = tagService.findByProjectPath(projectName, path);
	// PathSegment lastSegment = tagPath.getLast();
	// if (lastSegment != null) {
	// Tag tag = tagService.projectTo(lastSegment.getNode(), Tag.class);
	// if (tag == null) {
	// String message = i18n.get(rc, "tag_not_found_for_path", path);
	// throw new EntityNotFoundException(message);
	// }
	// languages.add(lastSegment.getLanguageTag());
	// // TODO handle update
	// rc.response().end(toJson(tagService.transformToRest(tag, languages)));
	// return;
	// } else {
	// throw new EntityNotFoundException(i18n.get(rc, "tag_not_found_for_path", path));
	// }
	// }

	// private void uuidPutHandler(RoutingContext rc) {

	// }

	private void addPathHandler() {

		// TODO add .produces(APPLICATION_JSON)
		pathRoute().method(GET).handler(rc -> {
			String path = rc.request().params().get("param0");
			String projectName = getProjectName(rc);
			List<String> languages = getSelectedLanguageTags(rc);

			// TODO findbyproject path should also handle files and contents and store the type of the segment
				Path tagPath = tagService.findByProjectPath(projectName, path);
				PathSegment lastSegment = tagPath.getLast();
				if (lastSegment != null) {

					// TODO last segment can also be a file or a content. Handle this
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
			});

	}

}
