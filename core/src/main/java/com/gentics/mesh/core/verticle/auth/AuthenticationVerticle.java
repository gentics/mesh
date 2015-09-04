package com.gentics.mesh.core.verticle.auth;

import static com.gentics.mesh.core.HttpConstants.APPLICATION_JSON;
import static com.gentics.mesh.util.VerticleHelper.transformAndResponde;
import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractCoreApiVerticle;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.rest.auth.LoginRequest;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.json.JsonUtil;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;

@Component
@Scope("singleton")
@SpringVerticle()
public class AuthenticationVerticle extends AbstractCoreApiVerticle {

	@Autowired
	private MeshSpringConfiguration springConfiguration;

	public AuthenticationVerticle() {
		super("auth");
	}

	@Override
	public void registerEndPoints() throws Exception {
		route("/me").handler(springConfiguration.authHandler());
		route("/me").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			ActionContext ac = ActionContext.create(rc);
			try (Trx tx = db.trx()) {
				MeshAuthUser requestUser = ac.getUser();
				transformAndResponde(ac, requestUser);
			}
		});

		route("/login").method(POST).consumes(APPLICATION_JSON).produces(APPLICATION_JSON).handler(rc -> {
			ActionContext ac = ActionContext.create(rc);
			try {
				LoginRequest request = JsonUtil.readValue(rc.getBodyAsString(), LoginRequest.class);
				// TODO fail on missing field
				JsonObject authInfo = new JsonObject().put("username", request.getUsername()).put("password", request.getPassword());
				springConfiguration.authProvider().authenticate(authInfo, rh -> {
					if (rh.failed()) {
						ac.fail(UNAUTHORIZED, "auth_login_failed", rh.cause());
					} else {
						User authenticated = rh.result();
						rc.setUser(authenticated);
						GenericMessageResponse message = new GenericMessageResponse("OK");
						ac.send(JsonUtil.toJson(message));
					}
				});
			} catch (Exception e) {
				ac.fail(UNAUTHORIZED, "auth_login_failed", e);
			}
		});

	}
}
