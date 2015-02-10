package com.gentics.cailun.nav.model;

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
import com.gentics.cailun.auth.CaiLunAuthServiceImpl;
import com.gentics.cailun.core.repository.GenericContentRepository;
import com.gentics.cailun.core.repository.TagRepository;
import com.gentics.cailun.core.rest.model.GenericNode;
import com.gentics.cailun.core.rest.model.Tag;
import com.gentics.cailun.core.rest.model.auth.BasicPermission;
import com.gentics.cailun.core.rest.model.auth.PermissionType;
import com.gentics.cailun.etc.CaiLunSpringConfiguration;
import com.gentics.cailun.util.Neo4jGenericContentUtils;

@Component
@Scope("singleton")
public class NavigationRequestHandler implements Handler<RoutingContext> {

	@Autowired
	TagRepository tagRepository;

	@Autowired
	CaiLunSpringConfiguration config;

	@Autowired
	Neo4jGenericContentUtils genericContentUtils;

	@Autowired
	GenericContentRepository genericContentRepository;

	private static ForkJoinPool pool = new ForkJoinPool(8);

	private Session session;

	public void handle(RoutingContext rc) {
		this.session = rc.session();
		Tag rootTag = tagRepository.findRootTag();
		try {
			Navigation nav = getNavigation(rootTag);
			rc.response().end(toJson(nav));
		} catch (Exception e) {
			// TODO error handling
			rc.fail(e);
			rc.fail(500);
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

	/**
	 * Returns a content navigation.
	 * 
	 * @param sess
	 * 
	 * @param rootTag
	 * @return
	 * @throws NodeNotFoundException
	 */
	private Navigation getNavigation(Tag rootTag) {

		Navigation nav = new Navigation();
		NavigationElement rootElement = new NavigationElement();
		rootElement.setName(rootTag.getName());
		rootElement.setType(NavigationElementType.TAG);
		nav.setRoot(rootElement);

		NavigationTask task = new NavigationTask(rootTag, rootElement, this, genericContentRepository, genericContentUtils);
		pool.invoke(task);
		return nav;
	}

	public void canView(GenericNode object, Handler<AsyncResult<Boolean>> resultHandler) {
		getAuthService().hasPermission(session.getPrincipal(), new BasicPermission(object, PermissionType.READ), resultHandler);
	}

	/**
	 * Wrapper for the permission checks. Check whether the given object can be viewed by the user.
	 * 
	 * @param object
	 * @return true, when the user can view the object. Otherwise false.
	 */
	public boolean canView(GenericNode object) {
		return getAuthService().hasPermission(session.getPrincipal(), new BasicPermission(object, PermissionType.READ));
	}
}
