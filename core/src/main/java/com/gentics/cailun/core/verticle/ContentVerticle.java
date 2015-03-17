package com.gentics.cailun.core.verticle;

import static com.gentics.cailun.util.JsonUtils.fromJson;
import static com.gentics.cailun.util.JsonUtils.toJson;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import io.vertx.ext.apex.core.Route;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.AbstractProjectRestVerticle;
import com.gentics.cailun.core.data.model.Content;
import com.gentics.cailun.core.data.model.Language;
import com.gentics.cailun.core.data.model.Tag;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.model.generic.GenericContent;
import com.gentics.cailun.core.data.service.ContentService;
import com.gentics.cailun.core.data.service.LanguageService;
import com.gentics.cailun.core.data.service.TagService;
import com.gentics.cailun.core.link.CaiLunLinkResolver;
import com.gentics.cailun.core.link.CaiLunLinkResolverFactoryImpl;
import com.gentics.cailun.core.link.LinkReplacer;
import com.gentics.cailun.core.rest.common.response.GenericMessageResponse;
import com.gentics.cailun.core.rest.content.request.ContentCreateRequest;
import com.gentics.cailun.core.rest.content.request.ContentUpdateRequest;
import com.gentics.cailun.error.EntityNotFoundException;
import com.gentics.cailun.error.HttpStatusCodeErrorException;

/**
 * The content verticle adds rest endpoints for manipulating contents.
 */
@Component
@Scope("singleton")
@SpringVerticle
public class ContentVerticle extends AbstractProjectRestVerticle {

	private static final Logger log = LoggerFactory.getLogger(ContentVerticle.class);

	@Autowired
	private ContentService contentService;

	@Autowired
	private TagService tagService;

	@Autowired
	private LanguageService languageService;

	@Autowired
	private CaiLunLinkResolverFactoryImpl<CaiLunLinkResolver> resolver;

	public ContentVerticle() {
		super("contents");
	}

	@Override
	public void registerEndPoints() throws Exception {
		addCRUDHandlers();
	}

	private void addCRUDHandlers() {
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

			Tag rootTagForContent = tagService.findByUUID(projectName, requestModel.getTagUuid());
			if (rootTagForContent == null) {
				// TODO i18n
				String message = "Root tag could not be found. Maybe it is not part of project {" + projectName + "}";
				throw new HttpStatusCodeErrorException(400, message);
			}
			failOnMissingPermission(rc, rootTagForContent, PermissionType.CREATE);

		});
	}

	private void addReadHandler() {
		Route readAllRoute = route("/").method(GET);
		readAllRoute.handler(rc -> {
			String projectName = getProjectName(rc);
			// TODO paging, filtering
				Iterable<Content> allContents = contentService.findAll(projectName);
			});

		Route route = route("/:uuid").method(GET);
		route.handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			String projectName = getProjectName(rc);

			Content content = contentService.findByUUID(projectName, uuid);
			if (content == null) {
				throw new EntityNotFoundException(i18n.get(rc, "content_not_found_for_uuid", uuid));
			}
			failOnMissingPermission(rc, content, PermissionType.READ);
			List<String> languageTags = getSelectedLanguageTags(rc);
			rc.response().end(toJson(contentService.transformToRest(content, languageTags)));
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
			// TODO i18n
			String message = "Deleted content with uuid {" + uuid + "}";
			rc.response().setStatusCode(200);
			rc.response().end(toJson(new GenericMessageResponse(message)));
		});
	}

	private void addUpdateHandler() {

		Route route = route("/:uuid").consumes(APPLICATION_JSON).method(PUT);
		route.handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			String projectName = getProjectName(rc);
			Content content = contentService.findByUUID(projectName, uuid);
			if (content == null) {
				throw new EntityNotFoundException(i18n.get(rc, "content_not_found_for_uuid", uuid));
			}
			ContentUpdateRequest request = fromJson(rc, ContentUpdateRequest.class);
			List<String> languageTags = getSelectedLanguageTags(rc);
		});
	}

	private void resolveLinks(GenericContent content) throws InterruptedException, ExecutionException {
		// TODO fix issues with generics - Maybe move the link replacer to a spring service
		// TODO handle language
		Language language = null;
		LinkReplacer replacer = new LinkReplacer(resolver);
		// content.setContent(language, replacer.replace(content.getContent(language)));
	}

}
