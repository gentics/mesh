package com.gentics.mesh.auth;

import com.gentics.mesh.auth.provider.MeshJWTAuthProvider;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthHandler;
import io.vertx.ext.web.handler.impl.AuthHandlerImpl;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Base64;

/**
 * Extended Vert.x {@link AuthHandler}.
 * 
 * The {@link #handle(RoutingContext)} method is overriden in order to support the {@link MeshJWTAuthProvider}.
 */
@Singleton
public class MeshBasicAuthLoginHandler extends AuthHandlerImpl {

	final String realm;

	private MeshJWTAuthProvider authProvider;

	@Inject
	public MeshBasicAuthLoginHandler(MeshJWTAuthProvider authProvider) {
		super(authProvider);
		this.authProvider = authProvider;
		this.realm = "Gentics Mesh";
	}

	private void authorizeUser(RoutingContext ctx, User user) {
		authorize(user, authZ -> {
			if (authZ.failed()) {
				ctx.fail(authZ.cause());
				return;
			}
			// success, allowed to continue
			ctx.next();
		});
	}

	@Override
	public void parseCredentials(RoutingContext context, Handler<AsyncResult<JsonObject>> handler) {
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
					String decoded = new String(Base64.getDecoder().decode(parts[1]));
					int colonIdx = decoded.indexOf(":");
					if (colonIdx != -1) {
						suser = decoded.substring(0, colonIdx);
						spass = decoded.substring(colonIdx + 1);
					} else {
						suser = decoded;
						spass = null;
					}
				} catch (ArrayIndexOutOfBoundsException e) {
					handle401(context);
					return;
				} catch (IllegalArgumentException | NullPointerException e) {
					// IllegalArgumentException includes PatternSyntaxException
					context.fail(e);
					return;
				}

				if (!"Basic".equals(sscheme)) {
					context.fail(400);
				} else {
					// We decoded the basic auth information and can now invoke the login call. The MeshAuthProvider will also set the JWT token in the cookie
					// and return the response to the requestor.
					InternalActionContext ac = new InternalRoutingActionContextImpl(context);
					authProvider.login(ac, suser, spass, null);
				}
			}
		}
	}

	private void handle401(RoutingContext context) {
		context.response().putHeader("WWW-Authenticate", "Basic realm=\"" + realm + "\"");
		context.fail(401);
	}

}
