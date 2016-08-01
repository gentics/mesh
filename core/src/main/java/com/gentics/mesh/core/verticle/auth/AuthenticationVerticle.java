package com.gentics.mesh.core.verticle.auth;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.AbstractCoreApiVerticle;
import com.gentics.mesh.rest.Endpoint;

@Component
@Scope("singleton")
@SpringVerticle()
public class AuthenticationVerticle extends AbstractCoreApiVerticle {

	@Autowired
	private AuthenticationRestHandler authRestHandler;

	public AuthenticationVerticle() {
		super("auth");
	}

	@Override
	public String getDescription() {
		return "Endpoint which contains login and logout methods.";
	}

	@Override
	public void registerEndPoints() throws Exception {

		// Only secure /me
		getRouter().route("/me").handler(getSpringConfiguration().authHandler());

		Endpoint meEndpoint = createEndpoint();
		meEndpoint.path("/me");
		meEndpoint.method(GET);
		meEndpoint.produces(APPLICATION_JSON);
		meEndpoint.description("Load your own user which is currently logged in.");
		meEndpoint.exampleResponse(200, userExamples.getUserResponse1("jdoe"));
		meEndpoint.handler(rc -> {
			authRestHandler.handleMe(InternalActionContext.create(rc));
		});

		Endpoint loginEndpoint = createEndpoint();
		loginEndpoint.path("/login");
		loginEndpoint.method(POST);
		loginEndpoint.consumes(APPLICATION_JSON);
		loginEndpoint.produces(APPLICATION_JSON);
		loginEndpoint.description("Login via this dedicated login endpoint");
		loginEndpoint.exampleRequest(miscExamples.getLoginRequest());
		loginEndpoint.exampleResponse(200, miscExamples.getAuthTokenResponse());
		loginEndpoint.handler(rc -> {
			authRestHandler.handleLogin(InternalActionContext.create(rc));
		});

		// Only secure logout
		getRouter().route("/logout").handler(getSpringConfiguration().authHandler());

		Endpoint logoutEndpoint = createEndpoint();
		logoutEndpoint.path("/logout");
		logoutEndpoint.method(GET);
		logoutEndpoint.produces(APPLICATION_JSON);
		logoutEndpoint.description("Logout and delete the currently active session.");
		logoutEndpoint.exampleResponse(200, miscExamples.getMessageResponse());
		logoutEndpoint.handler(rc -> {
			authRestHandler.handleLogout(InternalActionContext.create(rc));
		});
	}
}
