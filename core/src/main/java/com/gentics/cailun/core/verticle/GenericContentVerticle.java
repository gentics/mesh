package com.gentics.cailun.core.verticle;

import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.PUT;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.AbstractCailunRestVerticle;
import com.gentics.cailun.core.repository.GenericContentRepository;
import com.gentics.cailun.core.repository.TagRepository;
import com.gentics.cailun.core.rest.model.GenericContent;
import com.gentics.cailun.core.rest.model.Tag;
import com.gentics.cailun.core.rest.request.PageCreateRequest;
import com.gentics.cailun.core.rest.request.PageSaveRequest;
import com.gentics.cailun.core.rest.response.GenericResponse;

/**
 * The page verticle adds rest endpoints for manipulating pages and related objects.
 */
@Component
@Scope("singleton")
@SpringVerticle
public class GenericContentVerticle extends AbstractCailunRestVerticle {

	private static final Logger log = LoggerFactory.getLogger(GenericContentVerticle.class);

	@Autowired
	private GenericContentRepository genericContentRepository;

	@Autowired
	private TagRepository tagRepository;

	@Autowired
	GraphDatabaseService graphDb;

	public GenericContentVerticle() {
		super("contents");
	}

	@Override
	public void start() throws Exception {
		super.start();

		addSaveHandler();
		addLoadHandler();
		addDeleteHandler();
		addCreateHandler();

		// Tagging
		addAddTagHandler();
		addUntagPageHandler();
		addGetTagHandler();
	}

	private void addDeleteHandler() {
		route("/:uuid").method(DELETE).handler(rh -> {
			String uuid = rh.request().params().get("uuid");
			genericContentRepository.delete(uuid);
		});

	}

	/**
	 * Add a handler for removing a tag with a specific name from a page.
	 */
	private void addUntagPageHandler() {

		route("/:uuid/tags/:name").method(DELETE).handler(rh -> {
			String uuid = rh.request().params().get("uuid");
			String name = rh.request().params().get("name");
			rh.response().end(toJson(new GenericResponse<Tag>(genericContentRepository.untag(uuid, name))));
		});
	}

	/**
	 * Return the specific tag of a page.
	 */
	private void addGetTagHandler() {

		route("/:uuid/tags/:name").method(GET).handler(rh -> {
			String uuid = rh.request().params().get("uuid");
			String name = rh.request().params().get("name");
			rh.response().end(toJson(new GenericResponse<Tag>(genericContentRepository.getTag(uuid, name))));
		});

	}

	/**
	 * Add a tag to the page with id
	 */
	private void addAddTagHandler() {

		route("/:uuid/tags/:name").method(PUT).handler(rh -> {
			String uuid = rh.request().params().get("uuid");
			String name = String.valueOf(rh.request().params().get("name"));
			Tag tag = genericContentRepository.tagGenericContent(uuid, name);
			rh.response().end(toJson(new GenericResponse<Tag>(tag)));

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

		route("/:uuid").method(GET).handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			if (uuid != null) {
				GenericContent content = genericContentRepository.findByUUID(uuid);
				if (content != null) {
					rc.response().end(toJson(content));
				} else {
					rc.fail(404);
					rc.fail(new ContentNotFoundException(uuid));
				}
			}
		});

	}

	private void addSaveHandler() {

		// TODO change this to put once it works and update proxy and ajax call accordingly
		route("/:uuid").consumes(APPLICATION_JSON).method(PUT).handler(rc -> {
			String uuid = rc.request().params().get("uuid");

			PageSaveRequest request = fromJson(rc, PageSaveRequest.class);
			GenericContent content = genericContentRepository.findByUUID(uuid);
			if (content != null) {
				// content.setContent(request.getContent());
				genericContentRepository.save(content);
				GenericResponse<String> response = new GenericResponse<>();
				response.setObject("OK");
				rc.response().end(toJson(response));
			} else {
				rc.fail(404);
				rc.fail(new ContentNotFoundException(uuid));
			}

		});

	}

}
