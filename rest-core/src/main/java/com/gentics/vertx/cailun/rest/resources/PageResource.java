package com.gentics.vertx.cailun.rest.resources;

import io.vertx.core.Vertx;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.vertx.cailun.repository.Page;
import com.gentics.vertx.cailun.repository.PageRepository;
import com.gentics.vertx.cailun.repository.Tag;
import com.gentics.vertx.cailun.rest.model.request.PageCreateRequest;
import com.gentics.vertx.cailun.rest.model.response.GenericResponse;

/**
 * Simple page resource to load and render pages
 */
@Component
@Scope("singleton")
@Path("/page")
public class PageResource extends AbstractCaiLunResource {

	@Autowired
	private PageRepository pageRepository;

	/**
	 * Return the page with the given id
	 * 
	 * @param vertx
	 * @param id
	 * @return
	 * @throws Exception
	 */
	@GET
	@Path("{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Page getPage(@Context Vertx vertx, final @PathParam("id") Long id) throws Exception {
		if (id != null) {
			return pageRepository.findOne(id);
		} else {
			throw new Exception("Please specify a correct id.");
		}
	}

	/**
	 * Return a list of all pages in the graph
	 * 
	 * @return
	 * @throws Exception
	 */
	@GET
	@Path("/pages")
	@Produces(MediaType.APPLICATION_JSON)
	public GenericResponse<List<Page>> getPages() throws Exception {
		// TODO use paging here
		GenericResponse<List<Page>> response = new GenericResponse<List<Page>>();
		response.setObject(pageRepository.findAllPages());
		return response;
	}

	/**
	 * Add a tag to the page with id
	 * 
	 * @param vertx
	 * @param request
	 * @param name
	 * @param id
	 * @return
	 * @throws Exception
	 */
	@PUT
	@Path("/tag/{id}/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	public GenericResponse<Tag> addTag(@Context Vertx vertx, PageCreateRequest request, final @PathParam("name") String name,
			final @PathParam("id") Long id) throws Exception {
		Tag tag = pageRepository.tagPage(id, name);
		return new GenericResponse<Tag>(tag);

	}

	/**
	 * Remove the given tag from the page with id
	 * 
	 * @param vertx
	 * @param request
	 * @param name
	 * @param id
	 * @return
	 */
	@PUT
	@Path("untag/{id}/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	public GenericResponse<Tag> removeTag(@Context Vertx vertx, PageCreateRequest request, final @PathParam("name") String name,
			final @PathParam("id") Long id) {
		return pageRepository.untag(id, name);
	}

	/**
	 * Return the relation
	 * 
	 * @param id
	 * @param name
	 */
	@GET
	@Path("tag/{id}/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	public GenericResponse<Tag> getTag(final @PathParam("id") Long id, final @PathParam("name") String name) {
		return pageRepository.getTag(id, name);
	}

	/**
	 * Create a new page
	 * 
	 * @param vertx
	 * @param request
	 * @return
	 * @throws Exception
	 */
	@PUT
	@Produces(MediaType.APPLICATION_JSON)
	public GenericResponse<Page> createPage(@Context Vertx vertx, PageCreateRequest request) throws Exception {
		pageRepository.save(request.getPage());
		GenericResponse response = new GenericResponse<>();
		return response;
	}
}
