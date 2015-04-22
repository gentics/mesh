package com.gentics.cailun.core.verticle;

import static com.gentics.cailun.util.JsonUtils.toJson;
import static io.vertx.core.http.HttpMethod.GET;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.apex.Route;

import java.util.List;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.AbstractProjectRestVerticle;
import com.gentics.cailun.core.data.model.Tag;
import com.gentics.cailun.core.data.model.auth.CaiLunPermission;
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
		route("/*").handler(springConfiguration.authHandler());
		addPathHandler();
	}

	private Route pathRoute() {
		return getRouter().routeWithRegex("\\/(.*)");
	}

	private void addPathHandler() {

		// TODO add .produces(APPLICATION_JSON)
		pathRoute().method(GET).handler(rc -> {
			String path = rc.request().params().get("param0");
			String projectName = getProjectName(rc);
			List<String> languageTags = getSelectedLanguageTags(rc);

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
					rc.session().hasPermission(new CaiLunPermission(tag, PermissionType.READ).toString(), rh -> {
						
					});
					
					languageTags.add(lastSegment.getLanguageTag());
					rc.response().end(toJson(tagService.transformToRest(rc, tag, languageTags, 0)));
					return;
				} else {
					throw new EntityNotFoundException(i18n.get(rc, "tag_not_found_for_path", path));
				}
			});

	}

}
