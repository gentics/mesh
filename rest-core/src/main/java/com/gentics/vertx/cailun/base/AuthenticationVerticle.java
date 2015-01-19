package com.gentics.vertx.cailun.base;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.vertx.cailun.rest.response.GenericResponse;
import com.gentics.vertx.cailun.shiro.spring.SecurityConfiguration;

@Component
@Scope("singleton")
@Produces(MediaType.APPLICATION_JSON)
@Path("/auth")
public class AuthenticationVerticle {

	@Autowired
	SecurityConfiguration securityConfiguration;

	@Context
	SecurityContext securityContext;

	@POST
	@Path("login")
	public GenericResponse<String> login() {
		String username = "blub";
		String password = "blar";

		GenericResponse<String> response = new GenericResponse<>();
		response.setObject("OK");
		return response;
	}

	@GET
	@Path("principal")
	public String getUserPrincipal() {
		return securityContext.getUserPrincipal().getName();
	}
}
