package com.gentics.mesh.core.verticle.auth;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.auth.MeshBasicAuthLoginHandler;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.AbstractEndpoint;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.rest.Endpoint;

@Singleton
public class AuthenticationEndpoint extends AbstractEndpoint {

	private AuthenticationRestHandler authRestHandler;

	private MeshBasicAuthLoginHandler basicAuthLoginHandler;

	@Inject
	public AuthenticationEndpoint(RouterStorage routerStorage, AuthenticationRestHandler authRestHandler, MeshBasicAuthLoginHandler basicAuthHandler) {
		super("auth", routerStorage);
		this.authRestHandler = authRestHandler;
		this.basicAuthLoginHandler = basicAuthHandler;
	}

	public AuthenticationEndpoint() {
		super("auth", null);
	}

	@Override
	public String getDescription() {
		return "Endpoint which contains login and logout methods.";
	}

	@Override
	public void registerEndPoints() {

		// Only secure /me
		getRouter().route("/me").handler(authHandler);

		Endpoint meEndpoint = createEndpoint();
		meEndpoint.path("/me");
		meEndpoint.method(GET);
		meEndpoint.produces(APPLICATION_JSON);
		meEndpoint.description("Load your own user which is currently logged in.");
		meEndpoint.exampleResponse(OK, userExamples.getUserResponse1("jdoe"), "Currently logged in user.");
		meEndpoint.handler(rc -> {
			authRestHandler.handleMe(new InternalRoutingActionContextImpl(rc));
		});

		Endpoint basicAuthLoginEndpoint = createEndpoint();
		basicAuthLoginEndpoint.path("/login");
		basicAuthLoginEndpoint.method(GET);
		//basicAuthLoginEndpoint.produces(APPLICATION_JSON);
		basicAuthLoginEndpoint.description("Login via basic authentication.");
		basicAuthLoginEndpoint.exampleResponse(OK, "Login was sucessful");
		basicAuthLoginEndpoint.handler(basicAuthLoginHandler);

		Endpoint loginEndpoint = createEndpoint();
		loginEndpoint.path("/login");
		loginEndpoint.method(POST);
		loginEndpoint.consumes(APPLICATION_JSON);
		loginEndpoint.produces(APPLICATION_JSON);
		loginEndpoint.description("Login via this dedicated login endpoint.");
		loginEndpoint.exampleRequest(miscExamples.getLoginRequest());
		loginEndpoint.exampleResponse(OK, miscExamples.getAuthTokenResponse(), "Generated login token.");
		loginEndpoint.handler(rc -> {
			authRestHandler.handleLoginJWT(new InternalRoutingActionContextImpl(rc));
		});

		// Only secure logout
		getRouter().route("/logout").handler(authHandler);
		Endpoint logoutEndpoint = createEndpoint();
		logoutEndpoint.path("/logout");
		logoutEndpoint.method(GET);
		logoutEndpoint.produces(APPLICATION_JSON);
		logoutEndpoint.description("Logout and delete the currently active session.");
		logoutEndpoint.exampleResponse(OK, miscExamples.getMessageResponse(), "User was successfully logged out.");
		logoutEndpoint.handler(rc -> {
			authRestHandler.handleLogout(new InternalRoutingActionContextImpl(rc));
		});
	}
}
