package com.gentics.mesh.auth;

import com.gentics.mesh.auth.provider.MeshJWTAuthProvider;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.rest.auth.TokenResponse;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.shared.SharedKeys;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.impl.AuthenticationHandlerImpl;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

import java.util.Base64;

/**
 * Extended Vert.x {@link AuthHandler}.
 * 
 * The {@link #handle(RoutingContext)} method is overriden in order to support the {@link MeshJWTAuthProvider}.
 */
@Singleton
public class MeshBasicAuthLoginHandler extends AuthenticationHandlerImpl<MeshJWTAuthProvider> {

	final String realm;

	private MeshJWTAuthProvider authProvider;

	private final MeshOptions meshOptions;

	@Inject
	public MeshBasicAuthLoginHandler(MeshJWTAuthProvider authProvider, MeshOptions meshOptions) {
		super(authProvider);
		this.authProvider = authProvider;
		this.meshOptions = meshOptions;
		this.realm = "Gentics Mesh";
	}

	private void authorizeUser(RoutingContext ctx, User user) {
		// authorization is done with roles
		ctx.next();
	}

	@Override
	public void authenticate(RoutingContext routingContext, Handler<AsyncResult<User>> handler) {
		// Not needed
	}

	@Override
	public void handle(RoutingContext context) {
		User user = context.user();
		if (user != null) {
			// Already authenticated in, just authorise
			authorizeUser(context, user);
		} else {
			HttpServerRequest request = context.request();
			String authorization = request.headers().get(HttpHeaders.AUTHORIZATION);

			if (authorization == null) {
				handle401(context);
			} else {
				String suser;
				String spass;
				String sscheme;

				try {
					String[] parts = authorization.split(" ");
					sscheme = parts[0];

					if (StringUtils.equalsIgnoreCase("Basic", sscheme)) {
						String decoded = new String(Base64.getDecoder().decode(parts[1]));
						int colonIdx = decoded.indexOf(":");
						if (colonIdx != -1) {
							suser = decoded.substring(0, colonIdx);
							spass = decoded.substring(colonIdx + 1);
						} else {
							suser = decoded;
							spass = null;
						}

						// We decoded the basic auth information and can now invoke the login call. The MeshAuthProvider will also set the JWT token in the cookie
						// and return the response to the requestor.
						InternalActionContext ac = new InternalRoutingActionContextImpl(context);
						authProvider.login(ac, suser, spass, null);
					} else if (StringUtils.equalsIgnoreCase("Bearer", sscheme)) {
						// TODO check whether there are additional parts
						String token = parts[1];
						JsonObject authInfo = new JsonObject().put("token", token).put("options", new JsonObject());
						authProvider.authenticateJWT(authInfo, res -> {

							// Authentication was successful.
							if (res.succeeded()) {
								AuthenticationResult result = res.result();
								User authenticatedUser = result.getUser();
								context.setUser(authenticatedUser);

								InternalActionContext ac = new InternalRoutingActionContextImpl(context);
								String jwtToken = authProvider.generateToken(authenticatedUser);
								ac.addCookie(Cookie.cookie(SharedKeys.TOKEN_COOKIE_KEY, jwtToken)
										.setMaxAge(meshOptions.getAuthenticationOptions().getTokenExpirationTime()).setPath("/"));
								ac.send(new TokenResponse(jwtToken).toJson());
							} else {
								handle401(context);
							}
						});
					} else {
						context.fail(400);
					}
				} catch (ArrayIndexOutOfBoundsException e) {
					handle401(context);
					return;
				} catch (IllegalArgumentException | NullPointerException e) {
					// IllegalArgumentException includes PatternSyntaxException
					context.fail(e);
					return;
				}
			}
		}
	}

	private void handle401(RoutingContext context) {
		context.response().putHeader("WWW-Authenticate", "Basic realm=\"" + realm + "\"");
		context.fail(401);
	}

}
