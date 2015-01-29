package com.gentics.vertx.cailun.page;

import java.util.List;
import static io.vertx.core.http.HttpMethod.*;
import org.jacpfx.vertx.spring.SpringVerticle;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.vertx.cailun.base.rest.request.PageCreateRequest;
import com.gentics.vertx.cailun.base.rest.request.PageSaveRequest;
import com.gentics.vertx.cailun.page.model.Page;
import com.gentics.vertx.cailun.rest.AbstractCailunRestVerticle;
import com.gentics.vertx.cailun.rest.response.GenericResponse;
import com.gentics.vertx.cailun.tag.TagRepository;
import com.gentics.vertx.cailun.tag.model.Tag;

/**
 * The page verticle adds rest endpoints for manipulating pages and related objects.
 */
@Component
@Scope("singleton")
@SpringVerticle
public class PageVerticle extends AbstractCailunRestVerticle {

	private static final Logger log = LoggerFactory.getLogger(PageVerticle.class);

	@Autowired
	private PageRepository pageRepository;

	@Autowired
	private TagRepository tagRepository;

	@Autowired
	GraphDatabaseService graphDb;

	public PageVerticle() {
		super("page");
	}

	@Override
	public void start() throws Exception {
		super.start();

		// Page manipulation
		addSaveHandler();
		addLoadHandler();
		addGetPagesHandler();
		addCreateHandler();

		// Tagging
		addAddTagHandler();
		addUntagPageHandler();
		addGetTagHandler();
	}

	/**
	 * Add a handler for removing a tag with a specific name from a page.
	 */
	private void addUntagPageHandler() {

		route("/untag/:id/:name").method(DELETE).handler(rh -> {
			long id = Long.valueOf(rh.request().params().get("id"));
			String name = rh.request().params().get("name");
			rh.response().end(toJson(new GenericResponse<Tag>(pageRepository.untag(id, name))));
		});
	}

	/**
	 * Return the specific tag of a page.
	 */
	private void addGetTagHandler() {

		route("/tag/:id/:name").method(GET).handler(rh -> {
			long id = Long.valueOf(rh.request().params().get("id"));
			String name = String.valueOf(rh.request().params().get("name"));
			rh.response().end(toJson(new GenericResponse<Tag>(pageRepository.getTag(id, name))));
		});

	}

	/**
	 * Add a tag to the page with id
	 */
	private void addAddTagHandler() {

		route("/tag/:id/:name").method(PUT).handler(rh -> {
			long id = Long.valueOf(rh.request().params().get("id"));
			String name = String.valueOf(rh.request().params().get("name"));
			Tag tag = pageRepository.tagPage(id, name);
			rh.response().end(toJson(new GenericResponse<Tag>(tag)));

		});
	}

	/**
	 * Return a list of all pages in the graph
	 */
	private void addGetPagesHandler() {

		route("/pages").method(GET).handler(rc -> {
			// TODO use paging here
				GenericResponse<List<Page>> response = new GenericResponse<List<Page>>();
				response.setObject(pageRepository.findAllPages());
				rc.response().end(toJson(response));
			});
	}

	/**
	 * Add a page create handler
	 */
	private void addCreateHandler() {

		route().method(PUT).consumes(APPLICATION_JSON).handler(rc -> {
			PageCreateRequest request = fromJson(rc, PageCreateRequest.class);
			// TODO handle request
				rc.response().end(toJson(new GenericResponse<>()));
			});

	}

	/**
	 * Add the page load handler that allows loading pages by id.
	 */
	private void addLoadHandler() {

		route("/byId/:id").method(GET).handler(rc -> {
			String id = rc.request().params().get("id");
			//
			// if (id != null) {
			// return pageRepository.findOne(id);
			// } else {
			// throw new Exception("Please specify a correct id.");
			// }
				rc.response().end(toJson(pageRepository.findOne(Long.valueOf(id))));
			});

	}

	private void addSaveHandler() {

		// TODO change this to put once it works and update proxy and ajax call accordingly
		route("/save/:id").consumes(APPLICATION_JSON).method(POST).handler(rc -> {
			long id = Long.valueOf(rc.request().params().get("id"));

			PageSaveRequest request = fromJson(rc, PageSaveRequest.class);
			Page page = pageRepository.findOne(id);
			if (page != null) {
				page.setContent(request.getContent());
				pageRepository.save(page);
			}
			GenericResponse<String> response = new GenericResponse<>();
			response.setObject("OK");
			rc.response().end(toJson(response));

		});

	}

}
