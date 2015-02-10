package com.gentics.cailun.core.verticle;

import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.PUT;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.neo4j.graphdb.GraphDatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.AbstractCaiLunProjectRestVerticle;
import com.gentics.cailun.core.repository.ContentRepository;
import com.gentics.cailun.core.repository.TagRepository;
import com.gentics.cailun.core.rest.model.Content;
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
public class ContentVerticle extends AbstractCaiLunProjectRestVerticle {

	@Autowired
	private ContentRepository contentRepository;

	@Autowired
	private TagRepository tagRepository;

	@Autowired
	GraphDatabaseService graphDb;

	public ContentVerticle() {
		super("contents");
	}

	@Override
	public void registerEndPoints() throws Exception {
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
			contentRepository.delete(uuid);
		});

	}

	/**
	 * Add a handler for removing a tag with a specific name from a page.
	 */
	private void addUntagPageHandler() {

		route("/:uuid/tags/:name").method(DELETE).handler(rh -> {
			String uuid = rh.request().params().get("uuid");
			String name = rh.request().params().get("name");
			rh.response().end(toJson(new GenericResponse<Tag>(contentRepository.untag(uuid, name))));
		});
	}

	/**
	 * Return the specific tag of a page.
	 */
	private void addGetTagHandler() {

		route("/:uuid/tags/:name").method(GET).handler(rh -> {
			String uuid = rh.request().params().get("uuid");
			String name = rh.request().params().get("name");
			rh.response().end(toJson(new GenericResponse<Tag>(contentRepository.getTag(uuid, name))));
		});

	}

	/**
	 * Add a tag to the page with id
	 */
	private void addAddTagHandler() {

		route("/:uuid/tags/:name").method(PUT).handler(rh -> {
			String uuid = rh.request().params().get("uuid");
			String name = String.valueOf(rh.request().params().get("name"));
			Tag tag = contentRepository.tagGenericContent(uuid, name);
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
				Content content = contentRepository.findByUUID(uuid);
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

		route("/:uuid").consumes(APPLICATION_JSON).method(PUT).handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			PageSaveRequest request = fromJson(rc, PageSaveRequest.class);
			Content content = contentRepository.findByUUID(uuid);
			if (content != null) {
				content.setContent(request.getContent());
				contentRepository.save(content);
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
