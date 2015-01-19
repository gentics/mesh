package com.gentics.vertx.cailun.page;

import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import io.vertx.core.Vertx;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.server.rest.web.NodeNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.vertx.cailun.base.rest.request.PageCreateRequest;
import com.gentics.vertx.cailun.base.rest.request.PageSaveRequest;
import com.gentics.vertx.cailun.nav.model.Navigation;
import com.gentics.vertx.cailun.nav.model.NavigationElement;
import com.gentics.vertx.cailun.nav.model.NavigationElementType;
import com.gentics.vertx.cailun.page.model.Page;
import com.gentics.vertx.cailun.rest.AbstractCailunRestVerticle;
import com.gentics.vertx.cailun.rest.response.GenericResponse;
import com.gentics.vertx.cailun.tag.TagRepository;
import com.gentics.vertx.cailun.tag.model.Tag;
import com.gentics.vertx.cailun.tagcloud.model.TagCloud;
import com.gentics.vertx.cailun.tagcloud.model.TagCloudEntry;
import com.gentics.vertx.cailun.tagcloud.model.TagCloudResult;

/**
 * Simple page resource to load and render pages
 */
@Component
@Scope("singleton")
@SpringVerticle
public class PageVerticle extends AbstractCailunRestVerticle {
	
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
		addNavigationHandler();
		addTagCloudHandler();
		addPageSaveHandler();
		addPageLoadHandler();
		addGetPagesHandler();
		addCreatePageHandler();

	}

	private void addNavigationHandler() {
		route("/nav").method(GET).handler(rc -> {
			try {
				rc.response().end(toJson(getNavigation()));
			} catch (Exception e) {
				e.printStackTrace();
			}
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

	private void addCreatePageHandler() {
		route().method(PUT).consumes("application/json").handler(rc -> {
			// rc.request()
			// pageRepository.save(request.getPage());
			// return new GenericResponse<>();
			//
			});

	}

	private void addPageLoadHandler() {
		route("/byId/:id").method(GET).handler(rc -> {
			String id = rc.request().params().get("id");
			rc.response().end(toJson(getPageById(Long.valueOf(id))));
		});

	}

	private void addTagCloudHandler() {
		route("/tagcloud").method(GET).handler(rc -> {
			rc.response().end(toJson(getTagCloud()));
		});
	}

	private void addPageSaveHandler() {

		route("/save/:id").consumes("application/json").method(POST).handler(rc -> {
			// TODO change this to put once it works and update proxy and ajax call accordingly
			});

	}

	public Navigation getNavigation() throws NodeNotFoundException {

		// ExecutionEngine engine = new ExecutionEngine(graphDb);
		// String query = "MATCH (tag:Tag {name: 'www'}),rels =(page:Page)-[:TAGGED*1..2]-(tag) return rels";

		Tag rootTag = tagRepository.findRootTag();
		Navigation nav = new Navigation();
		NavigationElement rootElement = new NavigationElement();
		rootElement.setName(rootTag.getName());
		rootElement.setType(NavigationElementType.TAG);

		nav.setRoot(rootElement);
		traverse(rootTag, rootElement);

		return nav;
	}

	public TagCloud getTagCloud() {
		TagCloud cloud = new TagCloud();
		// TODO transaction handling should be moved to abstract rest resource
		try (Transaction tx = graphDb.beginTx()) {
			List<TagCloudResult> res = pageRepository.getTagCloudInfo();
			for (TagCloudResult current : res) {
				TagCloudEntry entry = new TagCloudEntry();
				entry.setName(current.getTag().getName());
				// TODO determine link
				entry.setLink("TBD");
				entry.setCount(current.getCounts());
				cloud.getEntries().add(entry);
			}
		}
		return cloud;
	}

	public Page getPageById(final @PathParam("id") Long id) {
		return pageRepository.findOne(id);
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

}
