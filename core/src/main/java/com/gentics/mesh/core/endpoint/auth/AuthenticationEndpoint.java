package com.gentics.mesh.core.endpoint.auth;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import javax.inject.Inject;

import com.gentics.mesh.auth.MeshAuthChain;
import com.gentics.mesh.auth.MeshBasicAuthLoginHandler;
import com.gentics.mesh.rest.InternalEndpointRoute;
import com.gentics.mesh.router.route.AbstractInternalEndpoint;

public class AuthenticationEndpoint extends AbstractInternalEndpoint {

	private AuthenticationRestHandler authRestHandler;

	private MeshBasicAuthLoginHandler basicAuthLoginHandler;

	@Inject
	public AuthenticationEndpoint(MeshAuthChain chain, AuthenticationRestHandler authRestHandler, MeshBasicAuthLoginHandler basicAuthHandler) {
		super("auth", chain);
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
		if (chain != null) {
			chain.secure(getRouter().route("/me"));
		}

		InternalEndpointRoute meEndpoint = createRoute();
		meEndpoint.path("/me");
		meEndpoint.method(GET);
		meEndpoint.produces(APPLICATION_JSON);
		meEndpoint.description("Load your own user which is currently logged in.");
		meEndpoint.exampleResponse(OK, userExamples.getUserResponse1("jdoe"), "Currently logged in user.");
		meEndpoint.blockingHandler(rc -> {
			authRestHandler.handleMe(wrap(rc));
		});

		InternalEndpointRoute basicAuthLoginEndpoint = createRoute();
		basicAuthLoginEndpoint.path("/login");
		basicAuthLoginEndpoint.method(GET);
		// basicAuthLoginEndpoint.produces(APPLICATION_JSON);
		basicAuthLoginEndpoint.description("Login via basic authentication.");
		basicAuthLoginEndpoint.exampleResponse(OK, "Login was sucessful");
		basicAuthLoginEndpoint.handler(basicAuthLoginHandler);

		InternalEndpointRoute loginEndpoint = createRoute();
		loginEndpoint.path("/login");
		loginEndpoint.method(POST);
		loginEndpoint.setMutating(false);
		loginEndpoint.consumes(APPLICATION_JSON);
		loginEndpoint.produces(APPLICATION_JSON);
		loginEndpoint.description("Login via this dedicated login endpoint.");
		loginEndpoint.exampleRequest(miscExamples.getLoginRequest());
		loginEndpoint.exampleResponse(OK, miscExamples.getAuthTokenResponse(), "Generated login token.");
		loginEndpoint.blockingHandler(rc -> {
			authRestHandler.handleLoginJWT(wrap(rc));
		});

		// Only secure logout
		if (chain != null) {
			chain.secure(getRouter().route("/logout"));
		}
		InternalEndpointRoute logoutEndpoint = createRoute();
		logoutEndpoint.path("/logout");
		logoutEndpoint.method(GET);
		logoutEndpoint.produces(APPLICATION_JSON);
		logoutEndpoint.description("Logout and delete the currently active session.");
		logoutEndpoint.exampleResponse(OK, miscExamples.createMessageResponse(), "User was successfully logged out.");
		logoutEndpoint.handler(rc -> {
			authRestHandler.handleLogout(wrap(rc));
		});
	}
}
