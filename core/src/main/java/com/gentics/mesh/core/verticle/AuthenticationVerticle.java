package com.gentics.mesh.core.verticle;

import static com.gentics.mesh.util.RoutingContextHelper.getUser;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import io.vertx.core.json.JsonObject;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractCoreApiVerticle;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.rest.auth.LoginRequest;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.json.JsonUtil;

@Component
@Scope("singleton")
@SpringVerticle()
public class AuthenticationVerticle extends AbstractCoreApiVerticle {

	@Autowired
	MeshSpringConfiguration springConfiguration;

	public AuthenticationVerticle() {
		super("auth");
	}

	@Override
	public void registerEndPoints() throws Exception {
		route("/me").handler(springConfiguration.authHandler());
		route("/me").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			MeshAuthUser requestUser = getUser(rc);
			transformAndResponde(rc, requestUser);
		});

		route("/login").method(POST).consumes(APPLICATION_JSON).produces(APPLICATION_JSON).handler(rc -> {
			LoginRequest request;
			try {
				request = JsonUtil.readValue(rc.getBodyAsString(), LoginRequest.class);
				//TODO fail on missing field
				JsonObject authInfo = new JsonObject().put("username", request.getUsername()).put("password", request.getPassword());
				springConfiguration.authProvider().authenticate(authInfo, rh -> {
					if (rh.failed()) {
						rc.fail(new HttpStatusCodeErrorException(401, i18n.get(rc, "login_failed"), rh.cause()));
					} else {
						GenericMessageResponse message = new GenericMessageResponse("OK");
						responde(rc, JsonUtil.toJson(message));
					}
				});
			} catch (Exception e) {
				rc.fail(new HttpStatusCodeErrorException(401, i18n.get(rc, "login_failed"), e));
			}
		});

	}
}
