package com.gentics.cailun.nav.model;

import io.vertx.core.Handler;
import io.vertx.ext.apex.RoutingContext;
import io.vertx.ext.apex.Session;

import java.util.concurrent.ForkJoinPool;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.cailun.core.data.model.Language;
import com.gentics.cailun.core.data.model.Tag;
import com.gentics.cailun.core.data.service.TagService;
import com.gentics.cailun.core.repository.generic.GenericNodeRepository;
import com.gentics.cailun.error.HttpStatusCodeErrorException;
import com.gentics.cailun.etc.CaiLunSpringConfiguration;
import com.gentics.cailun.util.Neo4jGenericContentUtils;

@Component
@Scope("singleton")
public class NavigationRequestHandler implements Handler<RoutingContext> {

	@Autowired
	private GenericNodeRepository<Tag> tagRepository;

	@Autowired
	private CaiLunSpringConfiguration config;

	@Autowired
	private TagService tagService;

	@Autowired
	private Neo4jGenericContentUtils genericContentUtils;

	private static ForkJoinPool pool = new ForkJoinPool(8);

	private Session session;

	public void handle(RoutingContext rc) {
		this.session = rc.session();
		// LocalizedTag rootTag = tagRepository.findRootTag();
		Tag rootTag = null;
		try {
			Navigation nav = getNavigation(rootTag);
			rc.response().end(toJson(nav));
		} catch (Exception e) {
			throw new HttpStatusCodeErrorException(500, "Could not build naviguation", e);
		}
	}

	private String toJson(Navigation navigation) throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(navigation);
	}

//	/**
//	 * Returns the cailun auth service which can be used to authenticate resources.
//	 * 
//	 * @return
//	 */
//	protected CaiLunAuthServiceImpl getAuthService() {
//		return config.authService();
//	}

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
		// TODO handle language
		Language language = null;
		Navigation nav = new Navigation();
		NavigationElement rootElement = new NavigationElement();
		String name = tagService.getName(rootTag, language);
		rootElement.setName(name);
		rootElement.setType(NavigationElementType.TAG);
		nav.setRoot(rootElement);

		// NavigationTask task = new NavigationTask(rootTag, rootElement, this, genericContentRepository, genericContentUtils);
		// pool.invoke(task);
		return nav;
	}

	// public void canView(GenericNode object, Handler<AsyncResult<Boolean>> resultHandler) {
	// getAuthService().hasPermission(session.getLoginID(), new CaiLunPermission(object, PermissionType.READ), resultHandler);
	// }

//	/**
//	 * Wrapper for the permission checks. Check whether the given object can be viewed by the user.
//	 * 
//	 * @param object
//	 * @return true, when the user can view the object. Otherwise false.
//	 */
//	public boolean canView(GenericNode object) {
//		return getAuthService().hasPermission(session.id(), new CaiLunPermission(object, PermissionType.READ));
//	}
}
