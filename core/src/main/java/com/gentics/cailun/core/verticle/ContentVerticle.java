package com.gentics.cailun.core.verticle;

import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import io.vertx.ext.apex.core.RoutingContext;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.AbstractProjectRestVerticle;
import com.gentics.cailun.core.data.model.Content;
import com.gentics.cailun.core.data.model.Language;
import com.gentics.cailun.core.data.model.Tag;
import com.gentics.cailun.core.data.model.generic.GenericContent;
import com.gentics.cailun.core.data.service.ContentService;
import com.gentics.cailun.core.data.service.LanguageService;
import com.gentics.cailun.core.data.service.TagService;
import com.gentics.cailun.core.link.CaiLunLinkResolver;
import com.gentics.cailun.core.link.CaiLunLinkResolverFactoryImpl;
import com.gentics.cailun.core.link.LinkReplacer;
import com.gentics.cailun.core.rest.common.response.GenericMessageResponse;
import com.gentics.cailun.core.rest.content.request.ContentUpdateRequest;
import com.gentics.cailun.core.rest.content.response.ContentResponse;
import com.gentics.cailun.util.UUIDUtil;

/**
 * The page verticle adds rest endpoints for manipulating pages and related objects.
 */
@Component
@Scope("singleton")
@SpringVerticle
public class ContentVerticle extends AbstractProjectRestVerticle {

	private static final Logger log = LoggerFactory.getLogger(ContentVerticle.class);

	private static final Object LANGUAGES_QUERY_PARAM_KEY = "lang";

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

		addPathHandler();

		addCreateHandler();
		// addReadHandler();
		addUpdateHandler();
		addDeleteHandler();

	}

	private void addDeleteHandler() {
		route("/:uuidOrName").method(DELETE).handler(rh -> {
			String uuidOrName = rh.request().params().get("uuidOrName");
			// contentRepository.delete(uuid);
			});

	}

	private void resolveLinks(GenericContent content) throws InterruptedException, ExecutionException {
		// TODO fix issues with generics - Maybe move the link replacer to a spring service
		// TODO handle language
		Language language = null;
		LinkReplacer replacer = new LinkReplacer(resolver);
		// content.setContent(language, replacer.replace(content.getContent(language)));
	}

	private void addPathHandler() {
		getRouter().routeWithRegex("\\/(.*)").method(GET).handler(rc -> {
			try {
				String projectName = getProjectName(rc);
				String path = rc.request().params().get("param0");
				// TODO remove debug code
				// TODO handle language by get parameter
				List<String> languages = getSelectedLanguages(rc);
				languages = new ArrayList<>();
				languages.add("english");

				// Determine whether the request path could be an uuid
				if (UUIDUtil.isUUID(path)) {
					String uuid = path;
					Content content = contentService.findByUUID(projectName, uuid);
					if (content != null) {
						handleResponse(rc, content, languages);
					} else {
						// TODO i18n error message?
						String message = "Content not found for uuid {" + uuid + "}";
						rc.response().setStatusCode(404);
						rc.response().end(toJson(new GenericMessageResponse(message)));
					}
					// Otherwise load the content by parsing and following the path
				} else {
					Content content = contentService.findByPath(projectName, "/" + path);
					if (content != null) {
						handleResponse(rc, content, languages);
					} else {
						// TODO i18n error message?
						String message = "Content not found for path {" + path + "}";
						rc.response().setStatusCode(404);
						rc.response().end(toJson(new GenericMessageResponse(message)));
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		});

	}

	private void handleResponse(RoutingContext rc, Content content, List<String> languages) {
		ContentResponse responseObject = contentService.getReponseObject(content, languages);
		// resolveLinks(content);
		String json = toJson(responseObject);
		rc.response().setStatusCode(200);
		rc.response().end(json);
	}

	/**
	 * Extracts the lang parameter values from the query
	 * 
	 * @param rc
	 * @return
	 */
	private List<String> getSelectedLanguages(RoutingContext rc) {
		String query = rc.request().query();
		Map<String, String> queryPairs;
		try {
			queryPairs = splitQuery(query);
		} catch (UnsupportedEncodingException e) {
			log.error("Could not decode query string.", e);
			return Collections.emptyList();
		}
		String value = queryPairs.get(LANGUAGES_QUERY_PARAM_KEY);
		if (value == null) {
			return Collections.emptyList();
		}
		return new ArrayList<>(Arrays.asList(value.split(",")));

	}

	/**
	 * Add a page create handler
	 */
	private void addCreateHandler() {

		getRouter().routeWithRegex("\\/(.*)").method(POST).consumes(APPLICATION_JSON).handler(rc -> {
			String projectName = getProjectName(rc);
			String path = rc.request().params().get("param0");
			ContentResponse requestModel = fromJson(rc, ContentResponse.class);
			if (requestModel == null) {
				String message = "Could not parse request";
				rc.response().setStatusCode(404);
				rc.response().end(toJson(new GenericMessageResponse(message)));
				return;
			} else {
				Tag rootTagForContent = tagService.findByProjectPath(projectName, path);
				if (rootTagForContent == null) {
					String message = "Could not find tag in path structure";
					rc.response().setStatusCode(400);
					rc.response().end(toJson(new GenericMessageResponse(message)));
				}
				Content content = null;
				try (Transaction tx = springConfig.getGraphDatabaseService().beginTx()) {
					content = contentService.save(projectName, path, requestModel);
					if (content != null) {
						rootTagForContent.addFile(content);
						tagService.save(rootTagForContent);
						tx.success();
					} else {
						tx.failure();
					}
				}
				if (content != null) {
					// Reload in order to update uuid field
					content = contentService.reload(content);
					// TODO simplify language handling - looks a bit chaotic
					Language language = languageService.findByLanguageTag(requestModel.getLanguageTag());
					// TODO check for npe - or see above
					handleResponse(rc, content, Arrays.asList(language.getName()));
					// rc.response().end("jow" + " " + path + " " + projectName);
				} else {
					rc.response().end("Could not save");
				}

			}
		});

		// route().method(PUT).consumes(APPLICATION_JSON).handler(rc -> {
		// // TODO handle request
		// // rc.response().end(toJson(new GenericResponse<>()));
		// });

	}

	private void addUpdateHandler() {

		route("/:uuid").consumes(APPLICATION_JSON).method(PUT).handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			ContentUpdateRequest request = fromJson(rc, ContentUpdateRequest.class);
			// Content content = contentRepository.findCustomerNodeBySomeStrangeCriteria(null);
			// if (content != null) {
			// content.setContent(request.getContent());
			// // contentRepository.save(content);
			// GenericResponse<String> response = new GenericResponse<>();
			// response.setObject("OK");
			// rc.response().end(toJson(response));
			// } else {
			// rc.fail(404);
			// rc.fail(new ContentNotFoundException(uuid));
			// }
			});

	}

}
