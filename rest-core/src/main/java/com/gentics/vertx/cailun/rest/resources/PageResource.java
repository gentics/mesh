package com.gentics.vertx.cailun.rest.resources;

import io.vertx.core.Vertx;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.server.rest.web.NodeNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.vertx.cailun.model.Page;
import com.gentics.vertx.cailun.model.Tag;
import com.gentics.vertx.cailun.model.nav.Navigation;
import com.gentics.vertx.cailun.model.nav.NavigationElement;
import com.gentics.vertx.cailun.model.nav.NavigationElementType;
import com.gentics.vertx.cailun.repository.PageRepository;
import com.gentics.vertx.cailun.repository.TagRepository;
import com.gentics.vertx.cailun.rest.model.request.PageCreateRequest;
import com.gentics.vertx.cailun.rest.model.request.PageSaveRequest;
import com.gentics.vertx.cailun.rest.model.response.GenericResponse;

/**
 * Simple page resource to load and render pages
 */
@Component
@Scope("singleton")
@Produces(MediaType.APPLICATION_JSON)
@Path("/page")
public class PageResource extends AbstractCaiLunResource {

	@Autowired
	private PageRepository pageRepository;

	@Autowired
	private TagRepository tagRepository;

	@Autowired
	GraphDatabaseService graphDb;

	@GET
	@Path("/nav")
	public Navigation getNavigration() throws NodeNotFoundException {

		ExecutionEngine engine = new ExecutionEngine(graphDb);
		// String query = "MATCH (tag:Tag {name: 'www'}),rels =(page:Page)-[:TAGGED*1..2]-(tag) return rels";

		Tag rootTag = tagRepository.findOne(2L);
		Navigation nav = new Navigation();
		NavigationElement rootElement = new NavigationElement();
		rootElement.setName(rootTag.getName());
		rootElement.setType(NavigationElementType.TAG);

		nav.setRoot(rootElement);
		traverse(rootTag, rootElement);

		return nav;
	}

	/**
	 * Recursively traverses the graph (depth-first) in order to populate the navigation elements
	 * 
	 * @param tag
	 * @param nav
	 */
	private void traverse(Tag tag, NavigationElement nav) {
		for (Object tagging : tag.getContents()) {
			if (tagging.getClass().isAssignableFrom(Page.class)) {
				Page page = (Page) tagging;
				NavigationElement pageNavElement = new NavigationElement();
				pageNavElement.setName(page.getFilename());
				pageNavElement.setType(NavigationElementType.PAGE);
				pageNavElement.setPath(pageRepository.getPath(page.getId()));
				nav.getChildren().add(pageNavElement);
			}
		}

		for (Tag currentTag : tag.getChildTags()) {
			NavigationElement navElement = new NavigationElement();
			navElement.setType(NavigationElementType.TAG);
			navElement.setName(currentTag.getName());
			nav.getChildren().add(navElement);
			traverse(currentTag, navElement);
		}
	}

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
	public Page getPage(@Context Vertx vertx, final @PathParam("id") Long id) throws Exception {
		if (id != null) {
			return pageRepository.findOne(id);
		} else {
			throw new Exception("Please specify a correct id.");
		}
	}

	// TODO change this to put once it works and update proxy and ajax call accordingly
	@POST
	//@Produces(MediaType.APPLICATION_JSON)
	//@Consumes(MediaType.APPLICATION_JSON)
	@Path("/save/{id}")
	public GenericResponse<String> savePage(final @PathParam("id") Long id, PageSaveRequest request) {
		Page page = pageRepository.findOne(id);
		if (page != null) {
			page.setContent(request.getContent());
			pageRepository.save(page);
		}
		GenericResponse<String> response = new GenericResponse<>();
		response.setObject("OK");
		return response;
	}

	/**
	 * Return a list of all pages in the graph
	 * 
	 * @return
	 * @throws Exception
	 */
	@GET
	@Path("/pages")
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
	public GenericResponse<Tag> removeTag(@Context Vertx vertx, PageCreateRequest request, final @PathParam("name") String name,
			final @PathParam("id") Long id) {
		return new GenericResponse<Tag>(pageRepository.untag(id, name));
	}

	/**
	 * Return the relation
	 * 
	 * @param id
	 * @param name
	 */
	@GET
	@Path("tag/{id}/{name}")
	public GenericResponse<Tag> getTag(final @PathParam("id") Long id, final @PathParam("name") String name) {
		return new GenericResponse<Tag>(pageRepository.getTag(id, name));
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
	public GenericResponse<Page> createPage(@Context Vertx vertx, PageCreateRequest request) throws Exception {
		pageRepository.save(request.getPage());
		return new GenericResponse<>();
	}
}
