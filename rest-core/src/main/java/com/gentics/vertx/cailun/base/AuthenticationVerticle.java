package com.gentics.vertx.cailun.base;

import static io.vertx.core.http.HttpMethod.GET;

import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.vertx.cailun.rest.AbstractCailunRestVerticle;
import com.gentics.vertx.cailun.rest.response.GenericResponse;

@Component
@Scope("singleton")
@SpringVerticle()
public class AuthenticationVerticle extends AbstractCailunRestVerticle {
	// @Autowired
	// SecurityConfiguration securityConfiguration;

	@Context
	SecurityContext securityContext;

	public AuthenticationVerticle() {
		super("auth");
	}

	@Override
	public void start() throws Exception {
		super.start();

		addLoginHandler();
		addUserInfoHandler();
	}

	private void addUserInfoHandler() {
		route("/principal").method(GET).handler(rc -> {
			rc.response().end(securityContext.getUserPrincipal().getName());
		});

	}

	private void addLoginHandler() {
		route("/login").consumes(APPLICATION_JSON).method(GET).handler(rc -> {
			String username = "blub";
			String password = "blar";

			GenericResponse<String> response = new GenericResponse<>();
			response.setObject("OK");
			rc.response().end(toJson(response));
		});

	}

}
