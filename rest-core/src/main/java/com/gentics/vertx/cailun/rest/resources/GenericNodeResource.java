package com.gentics.vertx.cailun.rest.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("singleton")
@Path("/graph")
@Produces(MediaType.APPLICATION_JSON)
public class GenericNodeResource extends AbstractCaiLunResource {
	
	@GET
	@Path("/id")
	public String getId() {
		return null;
	}

}
