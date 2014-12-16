package com.gentics.vertx.cailun.demo.resource;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.vertx.cailun.repository.TagRepository;
import com.gentics.vertx.cailun.rest.resources.AbstractCaiLunResource;

/**
 * Simple page resource to load and render pages
 */
@Component
@Scope("singleton")
@Path("/page")
public class CustomPageResource extends AbstractCaiLunResource {

	private static Logger log = LoggerFactory.getLogger(CustomPageResource.class);

//	@Autowired
//	private PageRepository pageRepository;

	@Autowired
	private TagRepository tagRepository;

	@GET
	@Path("id")
	@Produces(MediaType.APPLICATION_JSON)
	public String getId(@Context Vertx vertx) throws Exception {
		return "CustomPageResource";
	}

}
