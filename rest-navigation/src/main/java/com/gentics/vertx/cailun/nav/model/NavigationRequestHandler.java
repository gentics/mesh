package com.gentics.vertx.cailun.nav.model;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.ext.apex.core.RoutingContext;
import io.vertx.ext.apex.core.Session;

import java.util.concurrent.ForkJoinPool;

import org.neo4j.server.rest.web.NodeNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.vertx.cailun.auth.CaiLunAuthServiceImpl;
import com.gentics.vertx.cailun.auth.CaiLunConfiguration;
import com.gentics.vertx.cailun.base.model.GenericNode;
import com.gentics.vertx.cailun.page.PageRepository;
import com.gentics.vertx.cailun.perm.model.GenericPermission;
import com.gentics.vertx.cailun.tag.TagRepository;
import com.gentics.vertx.cailun.tag.model.Tag;

@Component
@Scope("singleton")
public class NavigationRequestHandler implements Handler<RoutingContext> {

	@Autowired
	private TagRepository tagRepository;

	@Autowired
	CaiLunConfiguration config;

	@Autowired
	private PageRepository pageRepository;

	private Session session;

	public void handle(RoutingContext rc) {
		this.session = rc.session();
		Tag rootTag = tagRepository.findRootTag();
		try {
			Navigation nav = getNavigation(rootTag);
			rc.response().end(toJson(nav));
		} catch (Exception e) {
			rc.response().end("Error");
		}
	}

	private String toJson(Navigation navigation) throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(navigation);

	}

	/**
	 * Returns the cailun auth service which can be used to authenticate resources.
	 * 
	 * @return
	 */
	protected CaiLunAuthServiceImpl getAuthService() {
		return config.authService();
	}

//	/**
//	 * Recursively traverses the graph (depth-first) in order to populate the navigation elements
//	 * 
//	 * @param tag
//	 * @param nav
//	 */
//	private void traverse(Tag tag, NavigationElement nav) {
//		// for (GenericNode tagging : tag.getContents()) {
//		tag.getContents().parallelStream().forEachOrdered(tagging -> {
//			if (tagging.getClass().isAssignableFrom(Page.class)) {
//				Page page = (Page) tagging;
//				if (canView(tag)) {
//					NavigationElement pageNavElement = new NavigationElement();
//					pageNavElement.setName(page.getFilename());
//					pageNavElement.setType(NavigationElementType.PAGE);
//					pageNavElement.setPath(pageRepository.getPath(page.getId()));
//					nav.getChildren().add(pageNavElement);
//				}
//			}
//		});
//
//		// for (Tag currentTag : tag.getChildTags()) {
//		tag.getChildTags().parallelStream().forEachOrdered(currentTag -> {
//			if (canView(currentTag)) {
//				NavigationElement navElement = new NavigationElement();
//				navElement.setType(NavigationElementType.TAG);
//				navElement.setName(currentTag.getName());
//				nav.getChildren().add(navElement);
//				traverse(currentTag, navElement);
//			}
//		});
//	}

	/**
	 * Returns a page navigation.
	 * 
	 * @param sess
	 * 
	 * @param rootTag
	 * @return
	 * @throws NodeNotFoundException
	 */
	private Navigation getNavigation(Tag rootTag) {

		// ExecutionEngine engine = new ExecutionEngine(graphDb);
		// String query = "MATCH (tag:Tag {name: 'www'}),rels =(page:Page)-[:TAGGED*1..2]-(tag) return rels";
		Navigation nav = new Navigation();
		NavigationElement rootElement = new NavigationElement();
		rootElement.setName(rootTag.getName());
		rootElement.setType(NavigationElementType.TAG);
		nav.setRoot(rootElement);

		// traverse(rootTag, rootElement);
		// http://www.javacodegeeks.com/2011/02/java-forkjoin-parallel-programming.html
		ForkJoinPool pool = new ForkJoinPool(10);
		NavigationTask task = new NavigationTask(rootTag, rootElement, this, pageRepository);
		pool.invoke(task);
		pool.shutdown();
		return nav;

	}

	public void canView(GenericNode object, Handler<AsyncResult<Boolean>> resultHandler) {
		getAuthService().hasPermission(session.getPrincipal(), new GenericPermission(object, "view"), resultHandler);
	}

	public boolean canView(GenericNode object) {
		return true;
		// return getAuthService().hasPermission(session.getPrincipal(), new GenericPermission(object, "view"));

	}
}
