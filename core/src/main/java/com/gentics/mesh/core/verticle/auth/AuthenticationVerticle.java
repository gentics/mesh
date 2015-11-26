package com.gentics.mesh.core.verticle.auth;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractCoreApiVerticle;
import com.gentics.mesh.handler.InternalActionContext;

@Component
@Scope("singleton")
@SpringVerticle()
public class AuthenticationVerticle extends AbstractCoreApiVerticle {

	@Autowired
	private AuthenticationRestHandler restHandler;

	public AuthenticationVerticle() {
		super("auth");
	}

	@Override
	public void registerEndPoints() throws Exception {
		route("/me").handler(springConfiguration.authHandler());
		route("/me").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			restHandler.handleMe(InternalActionContext.create(rc));
		});

		route("/login").method(POST).consumes(APPLICATION_JSON).produces(APPLICATION_JSON).handler(rc -> {
			restHandler.handleLogin(InternalActionContext.create(rc));
		});

		route("/logout").handler(springConfiguration.authHandler());
		route("/logout").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			restHandler.handleLogout(InternalActionContext.create(rc));
		});
	}
}
