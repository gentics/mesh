package com.gentics.cailun.core.verticle;

import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.PUT;
import io.vertx.ext.apex.core.RoutingContext;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.codehaus.jackson.map.ObjectMapper;
import org.jacpfx.vertx.spring.SpringVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.AbstractProjectRestVerticle;
import com.gentics.cailun.core.link.CaiLunLinkResolver;
import com.gentics.cailun.core.link.CaiLunLinkResolverFactoryImpl;
import com.gentics.cailun.core.link.LinkReplacer;
import com.gentics.cailun.core.rest.model.Content;
import com.gentics.cailun.core.rest.model.Language;
import com.gentics.cailun.core.rest.model.generic.GenericContent;
import com.gentics.cailun.core.rest.request.ContentCreateRequest;
import com.gentics.cailun.core.rest.request.ContentSaveRequest;
import com.gentics.cailun.core.rest.response.GenericContentResponse;
import com.gentics.cailun.core.rest.response.GenericNotFoundResponse;
import com.gentics.cailun.core.rest.service.ContentService;
import com.gentics.cailun.core.rest.service.TagService;
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
	private CaiLunLinkResolverFactoryImpl<CaiLunLinkResolver> resolver;

	public ContentVerticle() {
		super("contents");
	}

	@Override
	public void registerEndPoints() throws Exception {

		addCRUDHandlers();

		// Tagging
		// addAddTagHandler();
		// addUntagPageHandler();
		// addGetTagHandler();
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
						handleReponse(rc, content, languages);
					} else {
						// TODO i18n error message?
						String message = "Content not found for uuid {" + uuid + "}";
						rc.response().setStatusCode(404);
						rc.response().end(toJson(new GenericNotFoundResponse(message)));
					}
					// Otherwise load the content by parsing and following the path
				} else {
					Content content = contentService.findByProject(projectName, "/" + path);
					if (content != null) {
						handleReponse(rc, content, languages);
					} else {
						// TODO i18n error message?
						String message = "Content not found for path {" + path + "}";
						rc.response().setStatusCode(404);
						rc.response().end(toJson(new GenericNotFoundResponse(message)));
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		});

	}

	// /**
	// * Add a handler for removing a tag with a specific name from a page.
	// */
	// private void addUntagPageHandler() {
	//
	// route("/:uuid/tags/:name").method(DELETE).handler(rh -> {
	// String uuid = rh.request().params().get("uuid");
	// String name = rh.request().params().get("name");
	// rh.response().end(toJson(new GenericResponse<Tag>(contentRepository.untag(uuid, name))));
	// });
	// }

	// /**
	// * Return the specific tag of a page.
	// */
	// private void addGetTagHandler() {
	//
	// route("/:uuid/tags/:name").method(GET).handler(rh -> {
	// String uuid = rh.request().params().get("uuid");
	// String name = rh.request().params().get("name");
	// rh.response().end(toJson(new GenericResponse<Tag>(contentRepository.getTag(uuid, name))));
	// });
	//
	// }

	// /**
	// * Add a tag to the page with id
	// */
	// private void addAddTagHandler() {
	//
	// route("/:uuid/tags/:name").method(PUT).handler(rh -> {
	// String uuid = rh.request().params().get("uuid");
	// String name = String.valueOf(rh.request().params().get("name"));
	// Tag tag = contentRepository.tagGenericContent(uuid, name);
	// rh.response().end(toJson(new GenericResponse<Tag>(tag)));
	//
	// });
	// }

	private void handleReponse(RoutingContext rc, Content content, List<String> languages) {
		GenericContentResponse responseObject = contentService.getReponseObject(content, languages);
		// resolveLinks(content);
		String json = toJson(responseObject);
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

		route().method(PUT).consumes(APPLICATION_JSON).handler(rc -> {
			ContentCreateRequest request = fromJson(rc, ContentCreateRequest.class);
			// TODO handle request
			// rc.response().end(toJson(new GenericResponse<>()));
			});

	}

	// /**
	// * Add the page load handler that allows loading pages by id.
	// */
	// private void addReadHandler() {
	//
	// route("/:uuid").method(GET).handler(rc -> {
	// System.out.println("RCDATA:" + rc.contextData().get("cailun-project"));
	// String uuid = rc.request().params().get("uuid");
	// if (uuid != null) {
	// Content content = null;
	// if (content != null) {
	// ObjectMapper mapper = new ObjectMapper();
	// try {
	// rc.response().end(mapper.defaultPrettyPrintingWriter().writeValueAsString(content));
	// } catch (Exception e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// // rc.response().end(toJson(content));
	// } else {
	// rc.fail(404);
	// rc.fail(new ContentNotFoundException(uuid));
	// }
	// }
	// } );
	//
	// }

	private void addUpdateHandler() {

		route("/:uuid").consumes(APPLICATION_JSON).method(PUT).handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			ContentSaveRequest request = fromJson(rc, ContentSaveRequest.class);
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
