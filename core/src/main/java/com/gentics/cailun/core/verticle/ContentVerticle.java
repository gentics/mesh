package com.gentics.cailun.core.verticle;

import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.PUT;

import java.util.concurrent.ExecutionException;

import org.codehaus.jackson.map.ObjectMapper;
import org.jacpfx.vertx.spring.SpringVerticle;
import org.neo4j.graphdb.GraphDatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.AbstractCaiLunProjectRestVerticle;
import com.gentics.cailun.core.link.CaiLunLinkResolver;
import com.gentics.cailun.core.link.CaiLunLinkResolverFactoryImpl;
import com.gentics.cailun.core.link.LinkReplacer;
import com.gentics.cailun.core.repository.project.generic.ProjectGenericContentRepository;
import com.gentics.cailun.core.repository.project.generic.ProjectGenericFileRepository;
import com.gentics.cailun.core.repository.project.generic.ProjectGenericTagRepository;
import com.gentics.cailun.core.rest.model.Language;
import com.gentics.cailun.core.rest.model.generic.GenericContent;
import com.gentics.cailun.core.rest.model.generic.GenericFile;
import com.gentics.cailun.core.rest.request.ContentCreateRequest;
import com.gentics.cailun.core.rest.request.ContentSaveRequest;
import com.gentics.cailun.core.rest.response.GenericResponse;
import com.gentics.cailun.etc.RouterStorage;

/**
 * The page verticle adds rest endpoints for manipulating pages and related objects.
 */
@Component
@Scope("singleton")
@SpringVerticle
public class ContentVerticle extends AbstractCaiLunProjectRestVerticle {

	@Autowired
	private ProjectGenericContentRepository contentRepository;

	@Autowired
	private ProjectGenericFileRepository<GenericFile> fileRepository;

	@Autowired
	private ProjectGenericTagRepository tagRepository;

	@Autowired
	private CaiLunLinkResolverFactoryImpl<CaiLunLinkResolver> resolver;

	@Autowired
	GraphDatabaseService graphDb;

	public ContentVerticle() {
		super("contents");
	}

	@Override
	public void registerEndPoints() throws Exception {
		System.out.println("CONTENT:_" + contentRepository);

		addCRUDHandlers();

		// Tagging
		// addAddTagHandler();
		// addUntagPageHandler();
		// addGetTagHandler();
	}

	private void addCRUDHandlers() {

		addPathHandler();

		addSaveHandler();
		addLoadHandler();
		addDeleteHandler();
		addCreateHandler();

	}

	private void addDeleteHandler() {
		route("/:uuid").method(DELETE).handler(rh -> {
			String uuid = rh.request().params().get("uuid");
			// contentRepository.delete(uuid);
			});

	}

	private void resolveLinks(GenericContent content) throws InterruptedException, ExecutionException {
		// TODO fix issues with generics - Maybe move the link replacer to a spring service
		//TODO handle language
		Language language = null;
		LinkReplacer replacer = new LinkReplacer(resolver);
		//content.setContent(language, replacer.replace(content.getContent(language)));
	}



	private void addPathHandler() {
		getRouter().routeWithRegex("\\/(.*)").method(GET).handler(rc -> {
			try {
				String projectName = (String) rc.contextData().get(RouterStorage.PROJECT_CONTEXT_KEY);
				String path = rc.request().params().get("param0");
				GenericFile file = fileRepository.findByProject(projectName, path);
				// TODO check whether pageRepository.findAllByTraversal(startNode, traversalDescription) might be an alternative

				// TODO check whether file is a content or a binary file
				if (false) {
//					Content content = (Content) file;
//					resolveLinks(content);
					ObjectMapper mapper = new ObjectMapper();
//					String json = mapper.writeValueAsString(new GenericResponse<Content>(content));
//					rc.response().end(json);
				} else {
					rc.fail(new Exception("Page for path {" + path + "} could not be found."));
					// TODO add json response - Make error responses generic
					rc.response().end("Element not found");
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

	/**
	 * Add a page create handler
	 */
	private void addCreateHandler() {

		route().method(PUT).consumes(APPLICATION_JSON).handler(rc -> {
			ContentCreateRequest request = fromJson(rc, ContentCreateRequest.class);
			// TODO handle request
				rc.response().end(toJson(new GenericResponse<>()));
			});

	}

	/**
	 * Add the page load handler that allows loading pages by id.
	 */
	private void addLoadHandler() {

		route("/:uuid").method(GET).handler(rc -> {
			System.out.println("RCDATA:" + rc.contextData().get("cailun-project"));
			// System.out.println("request for project {" + project.getName() + "}");
				String uuid = rc.request().params().get("uuid");
				if (uuid != null) {
					GenericContent content = contentRepository.findCustomerNodeBySomeStrangeCriteria("null");
					if (content != null) {
						ObjectMapper mapper = new ObjectMapper();
						try {
							rc.response().end(mapper.defaultPrettyPrintingWriter().writeValueAsString(content));
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						// rc.response().end(toJson(content));
					} else {
						rc.fail(404);
						rc.fail(new ContentNotFoundException(uuid));
					}
				}
			});

	}

	private void addSaveHandler() {

		route("/:uuid").consumes(APPLICATION_JSON).method(PUT).handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			ContentSaveRequest request = fromJson(rc, ContentSaveRequest.class);
//			Content content = contentRepository.findCustomerNodeBySomeStrangeCriteria(null);
//			if (content != null) {
//				content.setContent(request.getContent());
//				// contentRepository.save(content);
//				GenericResponse<String> response = new GenericResponse<>();
//				response.setObject("OK");
//				rc.response().end(toJson(response));
//			} else {
//				rc.fail(404);
//				rc.fail(new ContentNotFoundException(uuid));
//			}
		});

	}

}
