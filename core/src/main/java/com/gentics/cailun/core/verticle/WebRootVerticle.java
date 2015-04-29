package com.gentics.cailun.core.verticle;

import static com.gentics.cailun.util.JsonUtils.toJson;
import static io.vertx.core.http.HttpMethod.GET;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.apex.Route;

import java.util.List;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.AbstractProjectRestVerticle;
import com.gentics.cailun.core.data.model.Content;
import com.gentics.cailun.core.data.model.Tag;
import com.gentics.cailun.core.data.model.auth.CaiLunPermission;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.model.generic.GenericPropertyContainer;
import com.gentics.cailun.core.data.service.LanguageService;
import com.gentics.cailun.core.data.service.TagService;
import com.gentics.cailun.core.data.service.WebRootService;
import com.gentics.cailun.error.EntityNotFoundException;
import com.gentics.cailun.error.HttpStatusCodeErrorException;
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
	private LanguageService languageService;

	@Autowired
	private WebRootService webrootService;

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

	// TODO findbyproject path should also handle files and contents and store the type of the segment
	// TODO last segment can also be a file or a content. Handle this
	private void addPathHandler() {

		pathRoute().method(GET).produces(APPLICATION_JSON).handler(rc -> {
			String path = rc.request().params().get("param0");
			String projectName = getProjectName(rc);
			List<String> languageTags = getSelectedLanguageTags(rc);

			vertx.executeBlocking((Future<GenericPropertyContainer> bch) -> {
				Path nodePath = webrootService.findByProjectPath(projectName, path);
				PathSegment lastSegment = nodePath.getLast();

				if (lastSegment != null) {
					try (Transaction tx = graphDb.beginTx()) {
						if (lastSegment.getNode().hasLabel(Tag.getLabel())) {
							Tag tag = tagService.projectTo(lastSegment.getNode(), Tag.class);
							if (tag == null) {
								String message = i18n.get(rc, "object_not_found_for_path", path);
								throw new EntityNotFoundException(message);
							}

							rc.session().hasPermission(new CaiLunPermission(tag, PermissionType.READ).toString(), rh -> {
								languageTags.add(lastSegment.getLanguageTag());
								if (rh.result()) {
									bch.complete(tag);
								} else {
									throw new HttpStatusCodeErrorException(403, i18n.get(rc, "error_missing_perm"));
								}
							});

						} else if (lastSegment.getNode().hasLabel(Content.getLabel())) {
							Content content = contentService.projectTo(lastSegment.getNode(), Content.class);
							if (content == null) {
								String message = i18n.get(rc, "object_not_found_for_path", path);
								throw new EntityNotFoundException(message);
							}

							rc.session().hasPermission(new CaiLunPermission(content, PermissionType.READ).toString(), rh -> {
								languageTags.add(lastSegment.getLanguageTag());
								if (rh.result()) {
									bch.complete(content);
								} else {
									bch.fail(new HttpStatusCodeErrorException(403, i18n.get(rc, "error_missing_perm")));
								}
							});
						} else {
							throw new EntityNotFoundException(i18n.get(rc, "object_not_found_for_path", path));
						}

						tx.success();
					}
				} else {
					throw new EntityNotFoundException(i18n.get(rc, "object_not_found_for_path", path));
				}
			}, arh -> {
				/* TODO copy this to all other handlers. We need to catch async errors as well elsewhere */
				if (arh.succeeded()) {
					GenericPropertyContainer container = arh.result();
					if (container instanceof Content) {
						rc.response().end(toJson(contentService.transformToRest(rc, (Content) container, languageTags, 0)));
					} else if (container instanceof Tag) {
						rc.response().end(toJson(tagService.transformToRest(rc, (Tag) container, languageTags, 0)));
					}
				} else {
					rc.fail(new HttpStatusCodeErrorException(500, "error", arh.cause()));
				}
			});

		});

	}

}
